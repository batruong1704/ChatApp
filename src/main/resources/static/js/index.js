document.addEventListener('DOMContentLoaded', (event) => {
    let stompClient = null;
    let username = null;
    let currentTab = 'lobby';
    let lobbyCreated = false;
    let userId = null;

    function connect(event) {
        username = document.querySelector('#nameInput').value.trim();

        if (username) {
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);

            stompClient.connect({}, onConnected, onError);
        }
        event.preventDefault();
    }

    function onConnected() {
        // Subscribe to the Public Topic
        stompClient.subscribe('/topic/public', onMessageReceived);

        // Subscribe to room creation topic
        stompClient.subscribe('/topic/roomCreated', onRoomCreated);

        stompClient.send("/app/chat.addUser",
            {},
            JSON.stringify({ sender: username, type: 'JOIN' })
        );

        // Tạo phòng lobby nếu chưa tồn tại
        if (!lobbyCreated) {
            stompClient.send("/app/chat.createRoom", {}, JSON.stringify({ name: "Lobby", membersId: [], id: "lobby" }));
            lobbyCreated = true;
        }

        // Subscribe to get users topic
        stompClient.subscribe('/topic/users', onUsersReceived);

        document.querySelector('.login').style.display = 'none';
        document.querySelector('.chatroom').style.display = 'block';
        document.querySelector('#selfName').innerText = username;
    }

    function onError(error) {
        console.error('Disconnect!');
    }

    function sendMessage(event) {
        event.preventDefault();
        const messageInput = document.querySelector('#messageInput');
        const messageContent = messageInput.value.trim();

        if (messageContent && stompClient) {
            const chatMessage = {
                sender: username,
                message: messageContent,
                type: 'CHAT',
                target: currentTab
            };

            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));

            messageInput.value = '';
        }
    }

    function onMessageReceived(payload) {
        const message = JSON.parse(payload.body);
        const targetTab = message.target || 'lobby';

        let messages = JSON.parse(sessionStorage.getItem(targetTab)) || [];
        messages.push(message);
        sessionStorage.setItem(targetTab, JSON.stringify(messages));

        if (targetTab === currentTab) {
            displayMessage(message);
        }
    }

    function displayMessage(message) {
        const messageContainer = document.querySelector('#messageContainer');
        const messageElement = document.createElement('div');

        if (message.type === 'JOIN') {
            messageElement.className = 'message-wrapper event-message';
            messageElement.innerHTML = `<div><strong>${message.sender}</strong> has joined!</div>`;
        } else if (message.type === 'LEAVE') {
            messageElement.className = 'message-wrapper event-message';
            messageElement.innerHTML = `<div><strong>${message.sender}</strong> has left!</div>`;
        } else {
            messageElement.className = 'message-wrapper chat-message';
            if (message.sender === username) {
                messageElement.classList.add('self-message');
                messageElement.innerHTML = `<div><strong>Me:</strong></div><div class="self-content">${message.message}</div>`;
            } else {
                messageElement.classList.add('other-message');
                messageElement.innerHTML = `<div><strong>${message.sender}:</strong></div><div class="other-content">${message.message}</div>`;
            }
        }

        messageContainer.appendChild(messageElement);
        messageContainer.scrollTop = messageContainer.scrollHeight;
    }

    function switchTab(event) {
        const target = event.target;
        if (target.tagName === 'LI') {
            const users = document.querySelectorAll('#userList li');
            users.forEach(user => {
                user.style.backgroundColor = '';
            });

            target.style.backgroundColor = '#f0f0f0';
            currentTab = target.getAttribute('target');

            const messageContainer = document.getElementById('messageContainer');
            messageContainer.innerHTML = '';

            const messages = JSON.parse(sessionStorage.getItem(currentTab)) || [];
            messages.forEach(message => {
                displayMessage(message);
            });
        }
    }

    document.querySelector('#login').addEventListener('submit', connect, true);
    document.querySelector('#messageForm').addEventListener('submit', sendMessage, true);
    document.getElementById('groupList').addEventListener('click', switchTab);

    function createNewRoom(event) {
        event.preventDefault();
        const newRoomName = document.querySelector('#newChatRoomPrivate').value.trim();
        onUsersReceived;
        if (newRoomName && stompClient) {
            const selectedUsers = Array.from(document.querySelectorAll('#addUserPrivateForm input[name="selectedUsers"]:checked')).map(checkbox => checkbox.value);

            selectedUsers.unshift(userId);
            const roomData = {
                name: newRoomName,
                membersId: selectedUsers
            };

            // Gửi yêu cầu tạo phòng mới
            stompClient.send("/app/chat.createRoom", {}, JSON.stringify(roomData));
            $('#addUserPrivate').modal('hide');
        }
    }

    document.querySelector('#addUserPrivateForm').addEventListener('submit', createNewRoom, true);

    function onUsersReceived(payload) {
        const users = JSON.parse(payload.body);
        for (const id in users) {
            if (users[id] === username) {
                userId = id;
                break;
            }
        }
        updateUserListForPrivateChat(users);

    }

    function updateUserListForPrivateChat(users) {
        const userListElement = document.querySelector('#addUserPrivateForm .userList');
        userListElement.innerHTML = '';

        for (const id in users) {
            if (id === userId) {
                continue;
            }

            const username = users[id];
            const userItemElement = document.createElement('div');
            userItemElement.className = 'user-item border p-2 mb-2 d-flex align-items-center';
            userItemElement.innerHTML = `
            <div class="flex-grow-1">${username}</div>
            <div>
                <input class="form-check-input" type="checkbox" id="${id}" name="selectedUsers" value="${id}">
                <label class="form-check-label" for="${id}"></label>
            </div>
        `;
            userListElement.appendChild(userItemElement);
        }

        const checkboxes = userListElement.querySelectorAll('.form-check-input');
        checkboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                if (this.checked) {
                    this.parentElement.parentElement.classList.add('bg-primary', 'text-white');
                } else {
                    this.parentElement.parentElement.classList.remove('bg-primary', 'text-white');
                }
            });
        });
    }



    $('#addUserPrivate').on('show.bs.modal', function (event) {
        stompClient.send("/app/chat.getUsers");
    });


    function onRoomCreated(payload) {
        const room = JSON.parse(payload.body);
        if (room !== null) { // Kiểm tra nếu room không phải là null
            const roomListElement = document.getElementById('groupList');
            const users = JSON.parse(payload.body);
            updateUserListForPrivateChat(users);

            const isUserInRoom = room.membersId.includes(userId);

            if (room.id === 'lobby' || isUserInRoom) {
                if (!Array.from(roomListElement.children).some(li => li.getAttribute('target') === room.id)) {
                    const roomItemElement = document.createElement('li');
                    roomItemElement.className = 'list-group-item';
                    roomItemElement.setAttribute('target', room.id);
                    roomItemElement.textContent = room.name;
                    roomListElement.appendChild(roomItemElement);
                }
            }
        } else {
            console.log("Received null room data, lobby already exists.");
        }
    }



});
