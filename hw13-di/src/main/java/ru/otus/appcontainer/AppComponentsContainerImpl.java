package ru.otus.appcontainer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

@SuppressWarnings("squid:S1068")
public class AppComponentsContainerImpl implements AppComponentsContainer {

    private final List<Object> appComponents = new ArrayList<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();

    /**
     * Создает новый экземпляр контейнера компонентов и инициализирует его
     * на основе указанного класса-конфигурации.
     *
     * @param initialConfigClass класс-конфигурация, помеченный аннотацией
     *                          {@link AppComponentsContainerConfig}
     * @throws IllegalArgumentException если переданный класс не является корректной конфигурацией
     * @throws RuntimeException если произошла ошибка при создании компонентов
     */
    public AppComponentsContainerImpl(Class<?> initialConfigClass) {
        processConfig(initialConfigClass);
    }

    /**
     * Обрабатывает конфигурационный класс: создает все компоненты, определенные в нем,
     * в порядке, указанном атрибутом {@link AppComponent#order()}.
     *
     * @param configClass класс-конфигурация для обработки
     * @throws IllegalArgumentException если класс не является корректной конфигурацией
     *                                  или содержит некорректные определения компонентов
     */
    private void processConfig(Class<?> configClass) {
        validateConfigClass(configClass);

        List<Method> componentMethods = getComponentMethodsSortedByOrder(configClass);
        Object configInstance = createConfigInstance(configClass);

        createComponents(componentMethods, configInstance);
    }

    /**
     * Проверяет, что переданный класс является корректной конфигурацией.
     * Класс должен быть помечен аннотацией {@link AppComponentsContainerConfig}.
     *
     * @param configClass класс для проверки
     * @throws IllegalArgumentException если класс не помечен требуемой аннотацией
     */
    private void validateConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not a config %s", configClass.getName()));
        }
    }

    /**
     * Возвращает список методов, помеченных аннотацией {@link AppComponent},
     * отсортированных по значению атрибута {@link AppComponent#order()}.
     *
     * @param configClass класс-конфигурация для поиска методов
     * @return отсортированный список методов-компонентов
     */
    private List<Method> getComponentMethodsSortedByOrder(Class<?> configClass) {
        return Arrays.stream(configClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(AppComponent.class))
                .sorted(Comparator.comparingInt(
                        method -> method.getAnnotation(AppComponent.class).order()))
                .toList();
    }

    /**
     * Создает экземпляр класса-конфигурации через конструктор по умолчанию.
     *
     * @param configClass класс-конфигурация
     * @return экземпляр класса-конфигурации
     * @throws RuntimeException если не удалось создать экземпляр
     */
    private Object createConfigInstance(Class<?> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create instance of config class: %s", configClass.getName()), e);
        }
    }

    /**
     * Создает все компоненты, используя указанные методы и экземпляр конфигурации.
     *
     * @param componentMethods методы для создания компонентов (должны быть отсортированы по order)
     * @param configInstance экземпляр класса-конфигурации
     * @throws IllegalArgumentException если обнаружены дублирующиеся имена компонентов
     * @throws RuntimeException если не удалось создать какой-либо компонент
     */
    private void createComponents(List<Method> componentMethods, Object configInstance) {
        for (Method method : componentMethods) {
            AppComponent annotation = method.getAnnotation(AppComponent.class);
            String componentName = annotation.name();

            validateComponentNameUniqueness(componentName);

            Object[] dependencies = resolveMethodDependencies(method);
            Object component = createComponentInstance(method, configInstance, dependencies);

            registerComponent(component, componentName);
        }
    }

    /**
     * Проверяет, что имя компонента является уникальным в рамках контейнера.
     *
     * @param componentName имя компонента для проверки
     * @throws IllegalArgumentException если компонент с таким именем уже существует
     */
    private void validateComponentNameUniqueness(String componentName) {
        if (appComponentsByName.containsKey(componentName)) {
            throw new IllegalArgumentException(String.format("Duplicate component name: %s", componentName));
        }
    }

    /**
     * Разрешает зависимости для указанного метода, находя соответствующие компоненты.
     *
     * @param method метод, параметры которого являются зависимостями
     * @return массив зависимостей для передачи в метод
     * @throws IllegalArgumentException если не удалось разрешить какую-либо зависимость
     */
    private Object[] resolveMethodDependencies(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] dependencies = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> dependencyType = parameterTypes[i];
            Object dependency = findSingleComponentByType(dependencyType);

            if (dependency == null) {
                throw new IllegalArgumentException(String.format(
                        "No suitable component found for dependency type: %s in method %s",
                        dependencyType.getName(), method.getName()));
            }

            dependencies[i] = dependency;
        }

        return dependencies;
    }

    /**
     * Создает экземпляр компонента, вызывая указанный метод с переданными зависимостями.
     *
     * @param method метод для создания компонента
     * @param configInstance экземпляр конфигурационного класса
     * @param dependencies зависимости для передачи в метод
     * @return созданный компонент
     * @throws RuntimeException если произошла ошибка при вызове метода
     */
    private Object createComponentInstance(Method method, Object configInstance, Object[] dependencies) {
        try {
            return method.invoke(configInstance, dependencies);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to create component: %s",
                            method.getAnnotation(AppComponent.class).name()),
                    e);
        }
    }

    /**
     * Регистрирует созданный компонент в контейнере.
     *
     * @param component созданный компонент
     * @param componentName имя компонента
     */
    private void registerComponent(Object component, String componentName) {
        appComponents.add(component);
        appComponentsByName.put(componentName, component);
    }

    /**
     * Находит единственный компонент, совместимый с указанным типом.
     * Если найдено более одного подходящего компонента или ни одного, возвращает null.
     *
     * @param type тип или интерфейс компонента для поиска
     * @return найденный компонент или null, если не найден ровно один подходящий
     */
    private Object findSingleComponentByType(Class<?> type) {
        List<Object> compatibleComponents = findCompatibleComponents(type);

        if (compatibleComponents.size() == 1) {
            return compatibleComponents.getFirst();
        }

        return null;
    }

    /**
     * Находит все компоненты, совместимые с указанным типом.
     * Компонент считается совместимым, если его класс является подтипом указанного типа.
     *
     * @param type тип или интерфейс для поиска совместимых компонентов
     * @return список совместимых компонентов (может быть пустым)
     */
    private List<Object> findCompatibleComponents(Class<?> type) {
        List<Object> compatibleComponents = new ArrayList<>();

        for (Object component : appComponents) {
            if (type.isAssignableFrom(component.getClass())) {
                compatibleComponents.add(component);
            }
        }

        return compatibleComponents;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException если найден более чем один компонент указанного типа
     *                                  или если не найден ни один компонент
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(Class<C> componentClass) {
        List<Object> compatibleComponents = findCompatibleComponents(componentClass);

        validateComponentCount(componentClass, compatibleComponents);

        return (C) compatibleComponents.getFirst();
    }

    /**
     * Проверяет, что количество найденных компонентов соответствует ожиданиям.
     * Должен быть найден ровно один компонент указанного типа.
     *
     * @param componentClass тип компонента
     * @param components список найденных компонентов
     * @throws IllegalArgumentException если не найден ни один компонент или найдено более одного
     */
    private void validateComponentCount(Class<?> componentClass, List<Object> components) {
        if (components.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No component found for type: %s", componentClass.getName()));
        }

        if (components.size() > 1) {
            throw new IllegalArgumentException(String.format(
                    "Found %d components of type %s, expected exactly 1", components.size(), componentClass.getName()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException если не найден компонент с указанным именем
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(String componentName) {
        Object component = appComponentsByName.get(componentName);

        if (component == null) {
            throw new IllegalArgumentException(String.format("No component found with name: %s", componentName));
        }

        return (C) component;
    }
}
