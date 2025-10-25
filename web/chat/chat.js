(function () {
        // ====== DOM ======
        var msgs = document.getElementById('msgs');
        var text = document.getElementById('text');
        var btnSend = document.getElementById('send');

        // Bandeja
        var convList = document.getElementById('conv-list');
        var convSearch = document.getElementById('conv-search');

        // Header / Composer / Empty
        var chatHeader = document.getElementById('chat-header');
        var emptyState = document.getElementById('empty-state');
        var composer = document.getElementById('composer');
        var contactNameEl = document.getElementById('contact-name');
        var presenceText = document.getElementById('presence-text');
        var headerAvatar = document.getElementById('peer-avatar');
        var btnClose = document.getElementById('btn-close-conv') || document.getElementById('btn-close');

        // ====== Estado ======
        var ws, pingTimer = null;
        var conversationId = null;
        var role = (typeof CURRENT_ROLE !== 'undefined') ? CURRENT_ROLE : 'RESIDENTE';
        var currentPeerId = null;
        var currentPeerName = '';

        var conversations = [];   // [{id,residenteId,agenteId,estado,peerName?,lastText?,lastTs?}]
        var unread = {};          // { convId: count }

        // ====== Utils ======
        function safeParse(json) {
                try {
                        return JSON.parse(json || '{}');
                } catch (e) {
                        return {};
                }
        }
        function fmtTime(iso) {
                if (!iso)
                        return '';
                try {
                        var d = new Date((iso + '').replace(' ', 'T'));
                        return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
                } catch (e) {
                        return '';
                }
        }
        function initialsFrom(name) {
                var n = (name || '').trim();
                if (!n)
                        return '?';
                var p = n.split(/\s+/);
                var a = (p[0] && p[0][0]) ? p[0][0] : '';
                var b = (p[1] && p[1][0]) ? p[1][0] : '';
                return (a + b || a || '?').toUpperCase();
        }
        function showChatUI(show) {
                if (chatHeader)
                        chatHeader.hidden = !show;
                if (msgs)
                        msgs.hidden = !show;
                if (composer)
                        composer.hidden = !show;
                if (emptyState) {
                        emptyState.hidden = !!show;
                        emptyState.style.display = show ? 'none' : '';
                }
        }
        function setPresence(text, online) {
                if (presenceText) {
                        presenceText.textContent = text || 'Desconectado';
                        presenceText.classList.toggle('off', !online);
                }
                var dot = chatHeader ? chatHeader.querySelector('.dot') : null;
                if (dot)
                        dot.classList.toggle('off', !online);
        }
        function setContactName(name) {
                currentPeerName = name || ('Usuario #' + (currentPeerId || ''));
                if (contactNameEl)
                        contactNameEl.textContent = currentPeerName;
                if (headerAvatar)
                        headerAvatar.textContent = initialsFrom(currentPeerName);
        }
        function scrollToBottom() {
                if (msgs)
                        msgs.scrollTop = msgs.scrollHeight;
        }

        // ====== Helpers visuales (no leídos) ======
        function toggleUnreadClass(el, isUnread) {
                if (!el)
                        return;
                if (isUnread)
                        el.classList.add('unread');
                else
                        el.classList.remove('unread');
        }
        function updateConvUnreadVisual(convId) {
                if (!convList)
                        return;
                var isUnread = (unread[convId] || 0) > 0;
                [].forEach.call(convList.querySelectorAll('.conv-item'), function (el) {
                        if (String(el.getAttribute('data-id')) === String(convId)) {
                                toggleUnreadClass(el.querySelector('.conv-last'), isUnread);
                        }
                });
        }

        // ====== Burbujas ======
        function addBubble(texto, mine, tsIso) {
                if (!msgs)
                        return;
                var wrap = document.createElement('div');
                wrap.className = 'msg ' + (mine ? 'msg-me' : 'msg-them');

                var body = document.createElement('div');
                body.className = 'msg-body';
                body.textContent = texto || '';
                wrap.appendChild(body);

                var meta = document.createElement('div');
                meta.className = 'msg-meta';
                var time = document.createElement('time');
                time.className = 'msg-time';
                if (tsIso)
                        time.setAttribute('datetime', tsIso);
                time.textContent = fmtTime(tsIso);
                meta.appendChild(time);
                wrap.appendChild(meta);

                msgs.appendChild(wrap);
                scrollToBottom();
        }

        // ====== WebSocket ======
        function connect() {
                var base = (typeof BASE !== 'undefined') ? BASE : '';
                var proto = (location.protocol === 'https:') ? 'wss' : 'ws';
                ws = new WebSocket(proto + '://' + location.host + base + '/ws/chat');

                ws.onopen = function () {
                        if (pingTimer)
                                clearInterval(pingTimer);
                        pingTimer = setInterval(function () {
                                if (ws && ws.readyState === 1)
                                        ws.send(JSON.stringify({type: 'PING'}));
                        }, 25000);
                };

                ws.onmessage = function (e) {
                        var m = safeParse(e.data);

                        if (m.type === 'WELCOME') {
                                role = m.role || role;
                                cargarBandeja();
                                return;
                        }

                        if (m.type === 'CONVERSATION_OPENED') {
                                cargarBandeja().finally(function () {
                                        activateConversation(m.conversationId, {focus: true});
                                });
                                return;
                        }

                        if (m.type === 'MESSAGE') {
                                // No auto-abrir si NO es la activa -> sumar badge + poner preview en negrita
                                if (String(conversationId) !== String(m.conversationId)) {
                                        unread[m.conversationId] = (unread[m.conversationId] || 0) + 1;
                                        updateConvBadge(m.conversationId);
                                        updateConvUnreadVisual(m.conversationId);
                                } else {
                                        // Si es la activa, pintar burbuja
                                        addBubble(m.text, m.fromRole === role, m.ts || m.tsIso);
                                }
                                // Actualizar preview/hora en la bandeja
                                updateConvLast(m.conversationId, m.text, m.ts || m.tsIso);
                                return;
                        }

                        // ====== PRESENCE (WebSocket) ======
                        if (m.type === 'PRESENCE') {
                                if (currentPeerId && String(m.userId) === String(currentPeerId)) {
                                        setPresence(m.online ? 'En línea' : 'Desconectado', !!m.online);
                                }
                                return;
                        }

                        if (m.type === 'ERROR' || m.error) {
                                alert('Error: ' + (m.message || m.error));
                        }
                };

                ws.onclose = function () {
                        if (pingTimer) {
                                clearInterval(pingTimer);
                                pingTimer = null;
                        }
                        setTimeout(connect, 1500);
                };
        }
        connect();

        // ====== Suscripción de presencia (WS) ======
        function subscribePresence(peerUserId) {
                if (!ws || ws.readyState !== 1 || !peerUserId)
                        return;
                try {
                        ws.send(JSON.stringify({type: 'SUBSCRIBE_PRESENCE', peerUserId: peerUserId}));
                } catch (e) {
                }
        }

        // ====== Bandeja ======
        function cargarBandeja() {
                if (typeof BASE === 'undefined' || !convList)
                        return Promise.resolve();
                return fetch(BASE + '/api/chat/mis-conversaciones')
                        .then(r => r.ok ? r.json() : [])
                        .then(list => {
                                conversations = Array.isArray(list) ? list : [];
                                renderBandeja();
                        })
                        .catch(() => {
                        });
        }

        function convPeerInfo(c) {
                if (!c)
                        return {peerId: null, label: '—'};
                if (c.peerName && String(c.peerName).trim()) {
                        return {peerId: (role === 'RESIDENTE' ? c.agenteId : c.residenteId), label: c.peerName};
                }
                if (role === 'RESIDENTE')
                        return {peerId: c.agenteId, label: c.agenteId ? ('Agente #' + c.agenteId) : 'Agente'};
                return {peerId: c.residenteId, label: c.residenteId ? ('Residente #' + c.residenteId) : 'Residente'};
        }

        function setActiveListItem(id) {
                if (!convList)
                        return;
                [].forEach.call(convList.querySelectorAll('.conv-item'), function (it) {
                        if (id && String(it.getAttribute('data-id')) === String(id))
                                it.classList.add('active');
                        else
                                it.classList.remove('active');
                });
        }

        function renderBandeja() {
                if (!convList)
                        return;
                convList.innerHTML = '';
                var q = (convSearch && convSearch.value) ? convSearch.value.toLowerCase().trim() : '';

                conversations.forEach(function (c) {
                        var peer = convPeerInfo(c), name = peer.label;
                        if (q && name.toLowerCase().indexOf(q) === -1)
                                return;

                        var tpl = document.getElementById('tpl-conv-item');
                        var node = tpl ? tpl.content.cloneNode(true) : null;

                        var btn = node ? node.querySelector('.conv-item') : document.createElement('button');
                        if (!node)
                                btn.className = 'conv-item';
                        btn.setAttribute('data-id', c.id);
                        btn.setAttribute('data-peer', peer.peerId || '');

                        var av = node ? node.querySelector('.avatar') : null;
                        if (!av) {
                                av = document.createElement('div');
                                av.className = 'avatar sm';
                                av.textContent = initialsFrom(name);
                                btn.insertBefore(av, btn.firstChild);
                        } else {
                                av.textContent = initialsFrom(name);
                        }

                        var nm = node ? node.querySelector('.conv-name') : null;
                        if (!nm) {
                                nm = document.createElement('div');
                                nm.className = 'conv-name';
                                btn.appendChild(nm);
                        }
                        nm.textContent = name;

                        var last = node ? node.querySelector('.conv-last') : null;
                        if (!last) {
                                last = document.createElement('div');
                                last.className = 'conv-last';
                                btn.appendChild(last);
                        }
                        last.textContent = c.lastText || '';

                        var tm = node ? node.querySelector('.conv-time') : null;
                        if (!tm) {
                                tm = document.createElement('div');
                                tm.className = 'conv-time';
                                btn.appendChild(tm);
                        }
                        tm.textContent = fmtTime(c.lastTs || '');

                        var badge = node ? node.querySelector('.conv-badge') : null;
                        if (!badge) {
                                badge = document.createElement('span');
                                badge.className = 'conv-badge';
                                btn.appendChild(badge);
                        }
                        var n = unread[c.id] || 0;
                        badge.hidden = !(n > 0);
                        badge.textContent = n;
                        // <<-- Negrita si hay no leídos
                        toggleUnreadClass(last, n > 0);

                        btn.onclick = function () {
                                activateConversation(c.id, {focus: true, peerId: peer.peerId, peerName: name});
                                setActiveListItem(c.id);
                        };
                        convList.appendChild(node ? node : btn);
                });
                setActiveListItem(conversationId);
        }

        function updateConvBadge(convId) {
                if (!convList)
                        return;
                [].forEach.call(convList.querySelectorAll('.conv-item'), function (el) {
                        if (String(el.getAttribute('data-id')) === String(convId)) {
                                var b = el.querySelector('.conv-badge');
                                if (!b)
                                        return;
                                var n = unread[convId] || 0;
                                b.hidden = !(n > 0);
                                b.textContent = n;
                        }
                });
        }
        function updateConvLast(convId, text, tsIso) {
                if (!convList)
                        return;
                [].forEach.call(convList.querySelectorAll('.conv-item'), function (el) {
                        if (String(el.getAttribute('data-id')) === String(convId)) {
                                var last = el.querySelector('.conv-last');
                                if (last)
                                        last.textContent = text || '';
                                var tm = el.querySelector('.conv-time');
                                if (tm)
                                        tm.textContent = fmtTime(tsIso || '');
                                // <<-- sincronizar estilo según no-leídos
                                toggleUnreadClass(last, (unread[convId] || 0) > 0);
                        }
                });
        }

        // ====== Activar conversación ======
        function activateConversation(id, opts) {
                opts = opts || {};
                conversationId = id;
                unread[id] = 0;               // marcar como leída
                updateConvBadge(id);
                updateConvUnreadVisual(id);   // quitar negrita del preview

                if (opts.peerId)
                        currentPeerId = opts.peerId;
                if (opts.peerName)
                        setContactName(opts.peerName);
                else {
                        for (var i = 0; i < conversations.length; i++) {
                                if (String(conversations[i].id) === String(id)) {
                                        var p = convPeerInfo(conversations[i]);
                                        currentPeerId = p.peerId;
                                        setContactName(p.label);
                                        break;
                                }
                        }
                }

                // Presencia por WS
                setPresence('Desconectado', false);
                subscribePresence(currentPeerId);

                // Mostrar UI
                showChatUI(true);

                // Cargar historial
                cargarHistorial();
                if (opts.focus && text)
                        text.focus();
        }

        // ====== Cerrar conversación (UI) ======
        function closeConversationUI() {
                conversationId = null;
                currentPeerId = null;
                setActiveListItem(null);
                setContactName('—');
                setPresence('Desconectado', false);
                showChatUI(false);
        }
        if (btnClose)
                btnClose.addEventListener('click', closeConversationUI);

        // ====== Historial ======
        function cargarHistorial() {
                if (!conversationId || typeof BASE === 'undefined' || !msgs)
                        return;
                var url = BASE + '/api/chat/mensajes?conversationId=' + conversationId + '&limit=200';

                fetch(url)
                        .then(r => r.ok ? r.json() : [])
                        .then(function (arr) {
                                msgs.innerHTML = '';
                                (arr || []).forEach(function (m) {
                                        var mine = (m.fromRole === role) || (String(m.fromRole).toUpperCase() === String(role).toUpperCase());
                                        addBubble(m.text, mine, m.ts || m.tsIso);
                                });
                                scrollToBottom();
                        })
                        .catch(() => {
                        });
        }

        // ====== Crear conversación (solo RESIDENTE, si hay modal en tu JSP) ======
        var btnNewConv = document.getElementById('btn-new-conv');
        var modal = document.getElementById('modal-new-conv');
        var modalBackdrop = modal ? modal.querySelector('.modal-backdrop') : null;
        var modalClose = modal ? modal.querySelector('.modal-close') : null;
        var modalCancel = document.getElementById('cancel-new-conv');
        var confirmNewConv = document.getElementById('confirm-new-conv');
        var agentList = document.getElementById('agent-list');
        var agentSearch = document.getElementById('agent-search');
        var onlyOnline = document.getElementById('only-online');

        function openModal() {
                if (modal)
                        modal.hidden = false;
        }
        function closeModal() {
                if (modal)
                        modal.hidden = true;
        }
        function setConfirmEnabled(en) {
                if (confirmNewConv)
                        confirmNewConv.disabled = !en;
        }

        function renderAgents(list) {
                if (!agentList)
                        return;
                agentList.innerHTML = '';
                setConfirmEnabled(false);
                (list || []).forEach(function (a) {
                        var li = document.createElement('li');
                        li.setAttribute('role', 'option');
                        li.setAttribute('data-id', a.id);
                        li.setAttribute('aria-selected', 'false');

                        var name = document.createElement('span');
                        name.className = 'agent-name';
                        name.textContent = a.nombre;
                        li.appendChild(name);
                        if (a.online) {
                                var b = document.createElement('span');
                                b.className = 'badge online';
                                b.textContent = 'en línea';
                                li.appendChild(b);
                        }

                        li.onclick = function () {
                                [].forEach.call(agentList.querySelectorAll('li[role=option]'), function (x) {
                                        x.setAttribute('aria-selected', 'false');
                                });
                                li.setAttribute('aria-selected', 'true');
                                setConfirmEnabled(true);
                                confirmNewConv.setAttribute('data-agent-id', a.id);
                                confirmNewConv.setAttribute('data-agent-name', a.nombre);
                        };
                        agentList.appendChild(li);
                });
                if (!list || !list.length) {
                        var li = document.createElement('li');
                        li.textContent = 'No hay guardias disponibles';
                        agentList.appendChild(li);
                }
        }

        function fetchAgents() {
                if (typeof BASE === 'undefined')
                        return Promise.resolve();
                var url = BASE + '/api/chat/agentes';
                if (onlyOnline && onlyOnline.checked)
                        url += '?online=1';
                return fetch(url)
                        .then(r => r.ok ? r.json() : [])
                        .then(function (list) {
                                var q = (agentSearch && agentSearch.value) ? agentSearch.value.toLowerCase().trim() : '';
                                if (q)
                                        list = list.filter(function (a) {
                                                return (a.nombre || '').toLowerCase().indexOf(q) !== -1;
                                        });
                                if (onlyOnline && onlyOnline.checked)
                                        list.forEach(function (a) {
                                                a.online = true;
                                        });
                                renderAgents(list);
                        })
                        .catch(function () {
                                renderAgents([]);
                        });
        }

        if (btnNewConv && modal) {
                btnNewConv.onclick = function () {
                        openModal();
                        fetchAgents();
                };
        }
        if (modalBackdrop)
                modalBackdrop.onclick = closeModal;
        if (modalClose)
                modalClose.onclick = closeModal;
        if (modalCancel)
                modalCancel.onclick = closeModal;
        if (onlyOnline)
                onlyOnline.onchange = fetchAgents;
        if (agentSearch)
                agentSearch.oninput = fetchAgents;

        if (confirmNewConv) {
                confirmNewConv.onclick = function () {
                        var agentId = parseInt(confirmNewConv.getAttribute('data-agent-id') || '0', 10);
                        var agentNm = confirmNewConv.getAttribute('data-agent-name') || '';
                        if (!agentId)
                                return;

                        fetch(BASE + '/api/chat/conversaciones', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({agentId: agentId})
                        })
                                .then(function (r) {
                                        if (!r.ok) {
                                                return r.text().then(function (txt) {
                                                        try {
                                                                var j = JSON.parse(txt || '{}');
                                                                throw new Error(j.error || j.message || 'No se pudo abrir conversación');
                                                        } catch (e) {
                                                                throw new Error(txt && txt.length < 200 ? txt : 'No se pudo abrir conversación');
                                                        }
                                                });
                                        }
                                        return r.json();
                                })
                                .then(function (j) {
                                        closeModal();
                                        activateConversation(j.conversationId, {focus: true, peerId: agentId, peerName: agentNm});
                                        if (ws && ws.readyState === 1)
                                                ws.send(JSON.stringify({type: 'OPEN_CONVERSATION', agentId: agentId}));
                                        cargarBandeja();
                                })
                                .catch(function (err) {
                                        if ((err.message || '').indexOf('Ya existe una conversacion') !== -1) {
                                                fetch(BASE + '/api/chat/mis-conversaciones')
                                                        .then(r => r.json())
                                                        .then(function (list) {
                                                                var cv = (list || []).find(function (c) {
                                                                        return c.agenteId === agentId && c.estado === 'ABIERTA';
                                                                });
                                                                if (cv) {
                                                                        closeModal();
                                                                        activateConversation(cv.id, {focus: true, peerId: agentId, peerName: agentNm});
                                                                } else {
                                                                        alert('Ya existe una conversación con ese guardia.');
                                                                }
                                                        })
                                                        .catch(function () {
                                                                alert('Ya existe una conversación.');
                                                        });
                                                return;
                                        }
                                        alert(err.message || 'No se pudo abrir conversación');
                                });
                };
        }

        // ====== Enviar ======
        if (btnSend) {
                btnSend.onclick = function () {
                        if (!conversationId)
                                return alert('Selecciona o crea una conversación primero');
                        var t = (text && text.value ? text.value : '').trim();
                        if (!t)
                                return;
                        if (ws && ws.readyState === 1) {
                                var payload = {type: 'SEND_MESSAGE', conversationId: conversationId, text: t};
                                ws.send(JSON.stringify(payload));
                        }
                        if (text)
                                text.value = '';
                };
        }
        if (text) {
                text.addEventListener('keydown', function (ev) {
                        if (ev.key === 'Enter' && !ev.shiftKey) {
                                ev.preventDefault();
                                if (btnSend)
                                        btnSend.click();
                        }
                });
        }

        // ====== Filtros/Buscador ======
        if (convSearch)
                convSearch.addEventListener('input', renderBandeja);

        // ====== Arranque ======
        showChatUI(false);
})();
