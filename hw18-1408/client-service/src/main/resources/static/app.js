let stompClient = null;
let currentRoomId = null;
let receivedMessages = new Set();
let messageCounter = 0;

const chatLineElementId = "chatLine";
const roomIdElementId = "roomId";
const messageElementId = "message";
const sendBtnElementId = "send";

const setConnected = (connected) => {
    const connectBtn = document.getElementById("connect");
    const disconnectBtn = document.getElementById("disconnect");
    const sendBtn = document.getElementById(sendBtnElementId);

    connectBtn.disabled = connected;
    disconnectBtn.disabled = !connected;
    const chatLine = document.getElementById(chatLineElementId);
    chatLine.hidden = !connected;

    // Блокируем кнопку отправки для комнаты 1408
    currentRoomId = document.getElementById(roomIdElementId).value;
    if (currentRoomId === "1408") {
        sendBtn.disabled = true;
        sendBtn.title = "Cannot send messages to room 1408";
    } else {
        sendBtn.disabled = false;
        sendBtn.title = "";
    }
}

const connect = () => {
    stompClient = Stomp.over(new SockJS('/gs-guide-websocket'));
    stompClient.connect({}, (frame) => {
        setConnected(true);
        const userName = frame.headers["user-name"];
        currentRoomId = document.getElementById(roomIdElementId).value;
        console.log(`Connected to roomId: ${currentRoomId} frame:${frame}`);

        // Очищаем историю полученных сообщений при подключении к новой комнате
        receivedMessages.clear();
        messageCounter = 0;

        // Очищаем чат
        const chatLine = document.getElementById(chatLineElementId);
        while (chatLine.rows.length > 0) {
            chatLine.deleteRow(0);
        }

        const topicName = `/topic/response.${currentRoomId}`;
        const topicNameUser = `/user/${userName}${topicName}`;

        // Подписываемся на обновления комнаты (новые сообщения в реальном времени)
        stompClient.subscribe(topicName, (message) => {
            const msg = JSON.parse(message.body).messageStr;
            // Создаем уникальный ключ для сообщения: текст + счетчик
            const messageKey = `${msg}_${messageCounter++}`;
            if (!receivedMessages.has(messageKey)) {
                receivedMessages.add(messageKey);
                showMessage(msg);
            }
        });

        // Подписываемся на личные обновления (исторические сообщения)
        stompClient.subscribe(topicNameUser, (message) => {
            const msg = JSON.parse(message.body).messageStr;
            // Для исторических сообщений используем другой формат ключа
            const messageKey = `history_${msg}`;
            if (!receivedMessages.has(messageKey)) {
                receivedMessages.add(messageKey);
                showMessage(msg);
            }
        });

        // Если это комната 1408, показываем специальное сообщение
        if (currentRoomId === "1408") {
            showMessage("=== Welcome to room 1408 ===");
            showMessage("This room shows ALL messages from ALL rooms");
            showMessage("You cannot send messages here");
        }
    });
}

const disconnect = () => {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    receivedMessages.clear();
    messageCounter = 0;
    console.log("Disconnected");
}

const sendMsg = () => {
    const roomId = document.getElementById(roomIdElementId).value;
    const message = document.getElementById(messageElementId).value;

    // Проверяем, что это не комната 1408
    if (roomId === "1408") {
        alert("Cannot send messages to room 1408");
        return;
    }

    if (!message.trim()) {
        alert("Message cannot be empty");
        return;
    }

    // Добавляем сообщение в локальный кэш перед отправкой
    const messageKey = `${message}_${messageCounter++}`;
    receivedMessages.add(messageKey);

    stompClient.send(`/app/message.${roomId}`, {}, JSON.stringify({'messageStr': message}))
    document.getElementById(messageElementId).value = "";

    // Показываем сообщение сразу в текущей комнате (если подключены к ней)
    if (currentRoomId === roomId) {
        showMessage(message);
    }
}

const showMessage = (message) => {
    const chatLine = document.getElementById(chatLineElementId);
    let newRow = chatLine.insertRow(-1);
    let newCell = newRow.insertCell(0);
    let newText = document.createTextNode(message);
    newCell.appendChild(newText);

    // Прокручиваем вниз к новому сообщению
    chatLine.scrollTop = chatLine.scrollHeight;
}

// Добавляем обработчик изменения комнаты
document.getElementById(roomIdElementId).addEventListener('change', function() {
    const roomId = this.value;
    const sendBtn = document.getElementById(sendBtnElementId);

    if (roomId === "1408") {
        sendBtn.disabled = true;
        sendBtn.title = "Cannot send messages to room 1408";
    } else {
        sendBtn.disabled = false;
        sendBtn.title = "";
    }

    // Если подключены, переподключаемся к новой комнате
    if (stompClient && stompClient.connected) {
        disconnect();
        setTimeout(connect, 100); // Небольшая задержка для чистого переподключения
    }
});

// Добавляем возможность отправки по Enter
document.getElementById(messageElementId).addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        sendMsg();
    }
});