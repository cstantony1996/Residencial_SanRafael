<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="java.util.List,Usuario.Usuario"%>
<%
        String ctx = request.getContextPath();
        List<Usuario> residentes = (List<Usuario>) request.getAttribute("residentes"); // puede ser null
%>
<!DOCTYPE html>
<html lang="es">
        <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Paquetería</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
                <link rel="stylesheet" href="<%=ctx%>/CSS/paqueteria.css">
        </head>
        <body>
                <div class="container">
                        <div class="header glass">
                                <div class="header-row">
                                        <h1><i class="fas fa-home"></i> Residencial San Rafael</h1>

                                        <a class="btn-glass-back" href="<%=ctx%>/vistas/menuGuardia.jsp">
                                                <span class="icon">
                                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                                                        <polyline points="9 22 9 12 15 12 15 22"/>
                                                        </svg>
                                                </span>
                                                <span class="text">Ir al inicio</span>
                                        </a>
                                </div>

                                <p>Gestión de Paquetería y Correspondencia</p>
                        </div>

                        <div class="main-content">
                                <!-- Formulario -->
                                <div class="card glass">
                                        <h2 class="card-title"><i class="fas fa-box"></i> Registrar Paquetería</h2>

                                        <div id="successAlert" class="alert alert-success" style="display:none;">
                                                <i class="fas fa-check-circle"></i><span>Información guardada con éxito</span>
                                        </div>

                                        <form id="packageForm" autocomplete="off">
                                                <div class="form-group">
                                                        <label for="numeroGuia"><i class="fas fa-barcode"></i> Número de Guía</label>
                                                        <input type="text" id="numeroGuia" name="numeroGuia" placeholder="Ingrese el número de guía" required>
                                                </div>

                                                <div class="form-group">
                                                        <label for="idUsuarioDest"><i class="fas fa-user"></i> Nombre Destinatario</label>
                                                        <select id="idUsuarioDest" name="idUsuarioDest" required>
                                                                <option value="">Seleccione un residente</option>
                                                                <%
                                                                        if (residentes != null) {
                                                                                for (Usuario r : residentes) {
                                                                                        String casa = (r.getLote() != null ? r.getLote() : "") + "-" + (r.getNumeroCasa() != null ? r.getNumeroCasa() : "");
                                                                %>
                                                                <option value="<%=r.getId()%>" data-casa="<%=casa%>"><%=r.getNombre()%> <%=r.getApellidos()%></option>
                                                                <%
                                                                                }
                                                                        }
                                                                %>
                                                        </select>
                                                </div>

                                                <div class="form-group">
                                                        <label for="numeroCasa"><i class="fas fa-house"></i> Número de Casa</label>
                                                        <input type="text" id="numeroCasa" placeholder="Se autocompletará" disabled readonly>
                                                </div>

                                                <div class="form-group">
                                                        <label for="observaciones"><i class="fas fa-note-sticky"></i> Observaciones (opcional)</label>
                                                        <input type="text" id="observaciones" name="observaciones" placeholder="Ej. recibido por DHL">
                                                </div>

                                                <div class="button-group">
                                                        <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Guardar</button>
                                                        <button type="button" class="btn btn-secondary" id="clearBtn"><i class="fas fa-eraser"></i> Limpiar</button>
                                                </div>
                                        </form>
                                </div>

                                <!-- Lista -->
                                <div class="card glass">
                                        <h2 class="card-title"><i class="fas fa-list"></i> Paquetes Pendientes</h2>

                                        <div id="searchContainer" class="search-box" style="display:none;">
                                                <input type="text" id="searchInput" placeholder="Buscar por guía, destinatario o casa...">
                                        </div>

                                        <div id="packageListContainer">
                                                <div class="empty-state">
                                                        <i class="fas fa-inbox"></i>
                                                        <h3>No hay paquetería pendiente de entregar</h3>
                                                        <p>Los paquetes registrados aparecerán aquí</p>
                                                </div>
                                        </div>
                                </div>
                        </div>
                </div>

                <!-- Modal -->
                <div id="confirmModal" class="modal">
                        <div class="modal-content glass">
                                <div class="modal-header"><i class="fas fa-exclamation-triangle"></i><h3>Confirmar Entrega</h3></div>
                                <div class="modal-body">¿Está seguro de realizar la entrega de este paquete?</div>
                                <div class="modal-buttons">
                                        <button class="btn btn-secondary" id="modalNoBtn"><i class="fas fa-times"></i> No</button>
                                        <button class="btn btn-primary" id="modalYesBtn"><i class="fas fa-check"></i> Sí</button>
                                </div>
                        </div>
                </div>

                <script>

                        var ctx = "<%=ctx%>";

// Referencias del DOM
                        var form = document.getElementById('packageForm');
                        var btnGuardar = form.querySelector('button[type="submit"]');
                        var selRes = document.getElementById('idUsuarioDest');
                        var numeroGuiaEl = document.getElementById('numeroGuia');
                        var observEl = document.getElementById('observaciones');
                        var casa = document.getElementById('numeroCasa');
                        var clearBtn = document.getElementById('clearBtn');

                        var success = document.getElementById('successAlert');
                        var listBox = document.getElementById('packageListContainer');
                        var searchCt = document.getElementById('searchContainer');
                        var searchIn = document.getElementById('searchInput');

                        var modal = document.getElementById('confirmModal');
                        var modalYes = document.getElementById('modalYesBtn');
                        var modalNo = document.getElementById('modalNoBtn');

                        var pendientes = [];
                        var paqueteEnConfirmacion = null;

                        console.log('🚀 Script inicializado');

// Funciones auxiliares
                        function toggleSubmitting(on) {
                                btnGuardar.disabled = on;
                                btnGuardar.style.opacity = on ? .7 : 1;
                                btnGuardar.style.cursor = on ? 'not-allowed' : 'pointer';
                        }

                        function esc(s) {
                                s = (s == null ? '' : ('' + s));
                                return s.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
                        }

// Función renderLista CON LOGS
                        function renderLista(filtro) {
                                console.log('🎨 renderLista() llamada');
                                console.log('   📊 Total pendientes:', pendientes.length);
                                console.log('   📊 Array:', JSON.stringify(pendientes, null, 2));
                                console.log('   🔍 Filtro:', filtro);

                                if (!pendientes.length) {
                                        console.log('   ⚠️ Array vacío, mostrando empty state');
                                        listBox.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><h3>No hay paquetería pendiente de entregar</h3><p>Los paquetes registrados aparecerán aquí</p></div>';
                                        searchCt.style.display = 'none';
                                        return;
                                }

                                searchCt.style.display = 'block';

                                var term = (filtro || '').toLowerCase();
                                var filtered = pendientes.filter(function (p) {
                                        var casaTxt = (p.lote || '') + '-' + (p.numeroCasa == null ? '' : p.numeroCasa);
                                        var nom = (p.nombre || '') + ' ' + (p.apellidos || '');
                                        return (p.numeroGuia || '').toLowerCase().includes(term) ||
                                                nom.toLowerCase().includes(term) ||
                                                casaTxt.toLowerCase().includes(term);
                                });

                                console.log('   ✅ Paquetes después de filtrar:', filtered.length);

                                if (filtered.length === 0) {
                                        listBox.innerHTML = '<div class="empty-state"><i class="fas fa-search"></i><h3>No se encontraron resultados</h3><p>Intente con otro término de búsqueda</p></div>';
                                        return;
                                }

                                var rows = filtered.map(function (p) {
                                        return '<tr data-id="' + esc(p.idPaquete) + '">' +
                                                '<td>' + esc(p.numeroGuia) + '</td>' +
                                                '<td>' + esc(p.nombre) + ' ' + esc(p.apellidos) + '</td>' +
                                                '<td>' + esc((p.lote || "") + '-' + (p.numeroCasa == null ? "" : p.numeroCasa)) + '</td>' +
                                                '<td>' + esc(p.fechaRecepcion) + '</td>' +
                                                '<td><button class="btn btn-deliver" onclick="abrirModal(' + Number(p.idPaquete) + ')"><i class="fas fa-truck"></i> Entregar</button></td>' +
                                                '</tr>';
                                }).join('');

                                listBox.innerHTML =
                                        '<table class="package-table">' +
                                        '<thead><tr>' +
                                        '<th>Número de Guía</th><th>Destinatario</th><th>Casa</th><th>Fecha Registro</th><th>Acción</th>' +
                                        '</tr></thead>' +
                                        '<tbody>' + rows + '</tbody>' +
                                        '</table>';

                                console.log('   ✅ Tabla HTML actualizada con', filtered.length, 'filas');
                        }

// Función cargarPendientes CON LOGS
                        function cargarPendientes(q) {
                                console.log('📡 cargarPendientes() llamada con q:', q);

                                var url = ctx + '/paqueteria/pendientes';
                                if (q && q.length)
                                        url += '?q=' + encodeURIComponent(q);

                                console.log('   🌐 Fetch a:', url);

                                fetch(url, {cache: 'no-store', credentials: 'same-origin'})
                                        .then(function (r) {
                                                return r.json();
                                        })
                                        .then(function (data) {
                                                console.log('   📥 Respuesta de /pendientes:', data);

                                                if (!data.ok) {
                                                        listBox.innerHTML = '<div class="empty-state"><i class="fas fa-triangle-exclamation"></i><h3>Error</h3><p>' + (data.error || 'No se pudo cargar') + '</p></div>';
                                                        return;
                                                }

                                                console.log('   ⚠️ SOBRESCRIBIENDO array pendientes');
                                                console.log('   📊 Antes:', pendientes.length, 'items');
                                                pendientes = data.pendientes || [];
                                                console.log('   📊 Después:', pendientes.length, 'items');

                                                renderLista(searchIn ? searchIn.value : '');
                                                searchCt.style.display = pendientes.length ? 'block' : 'none';
                                        })
                                        .catch(function (err) {
                                                console.error('   ❌ Error en fetch:', err);
                                                listBox.innerHTML = '<div class="empty-state"><i class="fas fa-triangle-exclamation"></i><h3>Error de red</h3><p>' + err + '</p></div>';
                                        });
                        }

// Event listeners simples
                        selRes.addEventListener('change', function () {
                                var opt = selRes.options[selRes.selectedIndex];
                                casa.value = (opt && opt.dataset ? opt.dataset.casa : '') || '';
                        });

                        clearBtn.addEventListener('click', function () {
                                form.reset();
                                casa.value = '';
                                success.style.display = 'none';
                        });

// Submit del formulario CON LOGS DETALLADOS
                        form.addEventListener('submit', function (e) {
                                e.preventDefault();
                                e.stopImmediatePropagation();

                                console.log('📝 SUBMIT del formulario');

                                var numeroGuia = (numeroGuiaEl.value || '').trim();
                                var idDest = (selRes.value || '').trim();
                                var observ = (observEl.value || '').trim();

                                if (!numeroGuia) {
                                        alert('El número de guía es obligatorio');
                                        return;
                                }
                                if (!idDest) {
                                        alert('Seleccione un residente');
                                        return;
                                }

                                toggleSubmitting(true);

                                var body = new URLSearchParams();
                                body.append('numeroGuia', numeroGuia);
                                body.append('idUsuarioDest', idDest);
                                body.append('observaciones', observ);

                                console.log('   🌐 POST a /paqueteria/registrar');
                                console.log('   📤 Datos:', {numeroGuia: numeroGuia, idDest: idDest, observ: observ});

                                fetch(ctx + '/paqueteria/registrar', {
                                        method: 'POST',
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                                        body: body,
                                        cache: 'no-store',
                                        credentials: 'same-origin'
                                })
                                        .then(function (r) {
                                                return r.text().then(function (t) {
                                                        console.log('   📥 Respuesta RAW:', t);
                                                        var data;
                                                        try {
                                                                data = JSON.parse(t);
                                                        } catch (_) {
                                                                data = {ok: false, error: t};
                                                        }
                                                        return {status: r.status, data: data};
                                                });
                                        })
                                        .then(function (res) {
                                                toggleSubmitting(false);

                                                console.log('   📊 Status:', res.status);
                                                console.log('   📊 Data:', res.data);

                                                if (res.status !== 200 || !res.data.ok) {
                                                        console.error('   ❌ Error del servidor');
                                                        alert((res.data && (res.data.error || res.data.mensaje)) || ('Error ' + res.status));
                                                        return;
                                                }

                                                console.log('   ✅ Guardado exitoso');
                                                console.log('   🆔 idPaquete recibido:', res.data.idPaquete);

                                                // Obtener datos del formulario
                                                var opt = selRes.options[selRes.selectedIndex];
                                                var nombreCompleto = opt ? opt.text : '';
                                                console.log('   👤 Nombre completo del select:', nombreCompleto);

                                                var piezas = nombreCompleto.split(' ');
                                                var primerNombre = piezas[0] || nombreCompleto;
                                                var apellidos = piezas.length > 1 ? piezas.slice(1).join(' ') : '';
                                                var lc = (casa.value || '').split('-');
                                                var lote = lc[0] || '';
                                                var nCasa = lc.length > 1 ? lc[1] : '';
                                                var fecha = new Date().toLocaleString('es-GT');

                                                var nuevoPaquete = {
                                                        idPaquete: res.data.idPaquete,
                                                        numeroGuia: numeroGuia,
                                                        nombre: primerNombre,
                                                        apellidos: apellidos,
                                                        lote: lote,
                                                        numeroCasa: nCasa,
                                                        fechaRecepcion: fecha
                                                };

                                                console.log('   📦 Nuevo paquete creado:', JSON.stringify(nuevoPaquete, null, 2));
                                                console.log('   📋 Array pendientes ANTES de agregar:', pendientes.length, 'items');

                                                // Agregar al inicio del array
                                                pendientes.unshift(nuevoPaquete);

                                                console.log('   📋 Array pendientes DESPUÉS de agregar:', pendientes.length, 'items');
                                                console.log('   📋 Contenido completo:', JSON.stringify(pendientes, null, 2));

                                                // Mostrar alerta de éxito
                                                success.style.display = 'flex';
                                                setTimeout(function () {
                                                        success.style.display = 'none';
                                                }, 1800);

                                                // Limpiar formulario
                                                form.reset();
                                                casa.value = '';

                                                console.log('   🎨 Llamando a renderLista("")...');

                                                // Renderizar inmediatamente
                                                renderLista('');

                                                console.log('   ✅ Proceso de guardado COMPLETADO');
                                        })
                                        .catch(function (err) {
                                                toggleSubmitting(false);
                                                console.error('   ❌ Error de red:', err);
                                                alert('Error de red: ' + err);
                                        });
                        });

// Búsqueda en vivo
                        if (searchIn)
                                searchIn.addEventListener('input', function (e) {
                                        renderLista(e.target.value);
                                });

// Modal de confirmación
                        window.abrirModal = function (idPaquete) {
                                paqueteEnConfirmacion = idPaquete;
                                modal.classList.add('active');
                        };

                        modalNo.addEventListener('click', function () {
                                modal.classList.remove('active');
                                paqueteEnConfirmacion = null;
                        });

                        modal.addEventListener('click', function (e) {
                                if (e.target === modal) {
                                        modal.classList.remove('active');
                                        paqueteEnConfirmacion = null;
                                }
                        });

// Confirmar entrega
                        modalYes.addEventListener('click', function () {
                                if (!paqueteEnConfirmacion)
                                        return;

                                var body = new URLSearchParams();
                                body.append('idPaquete', paqueteEnConfirmacion);

                                fetch(ctx + '/paqueteria/entregar', {
                                        method: 'POST',
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                                        body: body,
                                        cache: 'no-store',
                                        credentials: 'same-origin'
                                })
                                        .then(r => r.json())
                                        .then(data => {
                                                if (data.ok) {
                                                        // 1) Quitar del array (normalizando a String para evitar mismatch número/string)
                                                        var idStr = String(paqueteEnConfirmacion);
                                                        pendientes = pendientes.filter(p => String(p.idPaquete) !== idStr);

                                                        // 2) Quitar del DOM inmediato (feedback instantáneo)
                                                        var row = document.querySelector('tr[data-id="' + idStr + '"]');
                                                        if (row)
                                                                row.remove();

                                                        // 3) Re-render sin filtro raro
                                                        renderLista(searchIn ? searchIn.value : '');

                                                        // 4) Refrescar desde servidor para asegurar consistencia
                                                        cargarPendientes(searchIn ? searchIn.value : '');
                                                } else {
                                                        alert(data.error || data.mensaje || 'No se pudo marcar como entregado');
                                                }
                                        })
                                        .catch(err => {
                                                console.error('Error:', err);
                                                alert('Error de red: ' + err);
                                        })
                                        .finally(() => {
                                                modal.classList.remove('active');
                                                paqueteEnConfirmacion = null;
                                        });
                        });

// INIT: Cargar paquetes al iniciar
                        console.log('🔄 Cargando paquetes iniciales...');
                        cargarPendientes('');
                </script>


        </body>
</html>
