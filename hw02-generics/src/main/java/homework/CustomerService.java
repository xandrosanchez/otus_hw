package homework;

import java.util.AbstractMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class CustomerService {

    NavigableMap<Customer, String> customers;

    public Map.Entry<Customer, String> getSmallest() {
        if (customers.isEmpty()) {
            return null;
        }
        Map.Entry<Customer, String> smallest = customers.firstEntry();
        Customer clonedCustomer = smallest.getKey().clone();
        String resultValue = smallest.getValue();
        return new AbstractMap.SimpleEntry<>(clonedCustomer, resultValue);
    }

    public void add(Customer customer, String data) {
        if (customers == null) {
            customers = new TreeMap<>();
        }
        customers.put(customer, data);
    }

    public Map.Entry<Customer, String> getNext(Customer customer) {
        if (customers == null || customers.isEmpty()) {
            return null;
        }

        Map.Entry<Customer, String> result = customers.higherEntry(customer);
        if (result == null) {
            return null;
        }

        Customer clonedCustomer = result.getKey().clone();
        String resultValue = result.getValue();
        return new AbstractMap.SimpleEntry<>(clonedCustomer, resultValue);
    }
}
