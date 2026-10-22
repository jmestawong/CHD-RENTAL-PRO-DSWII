// Interfaz web de CH&D Rental Pro: consume la API REST del mismo servidor.
// El token JWT se guarda en sessionStorage y se envia en cada peticion.
(function () {
    'use strict';

    const $ = (sel, raiz = document) => raiz.querySelector(sel);
    const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const soles = (n) => n == null ? '—' : 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const fecha = (f) => f ? f.slice(0, 10).split('-').reverse().join('/') : '—';
    const hoyISO = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);

    // ------------------------------------------------------------ sesion y tema
    const sesion = {
        get token() { try { return sessionStorage.getItem('chd_token'); } catch { return null; } },
        get usuario() { try { return JSON.parse(sessionStorage.getItem('chd_usuario')); } catch { return null; } },
        guardar(token, usuario) {
            try { sessionStorage.setItem('chd_token', token); sessionStorage.setItem('chd_usuario', JSON.stringify(usuario)); } catch { /* sin almacenamiento */ }
            this._token = token; this._usuario = usuario;
        },
        limpiar() { try { sessionStorage.removeItem('chd_token'); sessionStorage.removeItem('chd_usuario'); } catch { /* nada */ } this._token = null; this._usuario = null; },
        tok() { return this._token || this.token; },
        usr() { return this._usuario || this.usuario; },
    };
    const rol = () => (sesion.usr() || {}).rol;
    const puede = {
        escribir: (recurso) => recurso === 'pagos' ? ['ADMIN', 'PAGOS'].includes(rol()) : ['ADMIN', 'OPERADOR', 'PAGOS'].includes(rol()),
        eliminar: () => rol() === 'ADMIN',
    };

    function aplicarTema(t) {
        document.documentElement.dataset.tema = t;
        try { localStorage.setItem('chd_tema', t); } catch { /* nada */ }
    }
    let temaGuardado = null;
    try { temaGuardado = localStorage.getItem('chd_tema'); } catch { /* nada */ }
    aplicarTema(temaGuardado || (matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'));
    document.querySelectorAll('.tema-toggle').forEach((b) => b.addEventListener('click', () =>
        aplicarTema(document.documentElement.dataset.tema === 'oscuro' ? 'claro' : 'oscuro')));

    // ------------------------------------------------------------ llamadas a la API
    class ErrorApi extends Error {
        constructor(status, body) {
            super((body && body.mensaje) || 'Error ' + status);
            this.status = status; this.errores = (body && body.errores) || {};
        }
    }
    async function api(metodo, ruta, cuerpo) {
        const headers = { 'Content-Type': 'application/json' };
        if (sesion.tok()) headers.Authorization = 'Bearer ' + sesion.tok();
        const r = await fetch(ruta, { method: metodo, headers, body: cuerpo ? JSON.stringify(cuerpo) : undefined });
        if (r.status === 204) return null;
        let data = null;
        try { data = await r.json(); } catch { /* respuesta sin cuerpo */ }
        if (r.status === 401 && ruta !== '/api/auth/login') {
            sesion.limpiar();
            mostrarLogin('Tu sesión expiró. Vuelve a iniciar sesión.');
            throw new ErrorApi(401, data);
        }
        if (!r.ok) throw new ErrorApi(r.status, data);
        return data;
    }

    function toast(msg, tipo = '') {
        const t = document.createElement('div');
        t.className = 'toast ' + tipo;
        t.textContent = msg;
        $('#toasts').appendChild(t);
        setTimeout(() => t.remove(), 4200);
    }

    // ------------------------------------------------------------ badges de estado
    const TONOS = {
        ACTIVO: 't-ok', DISPONIBLE: 't-ok', CONFIRMADO: 't-ok', COMPLETADO: 't-ok', FINALIZADO: 't-gris',
        ALQUILADO: 't-info', PENDIENTE: 't-warn', PROGRAMADO: 't-warn', EN_PROCESO: 't-info',
        MANTENIMIENTO: 't-warn', INACTIVO: 't-gris', CANCELADO: 't-error', ANULADO: 't-error',
        ADMIN: 't-error', OPERADOR: 't-info', PAGOS: 't-ok', GERENCIA: 't-warn',
    };
    const badge = (v) => v ? `<span class="badge ${TONOS[v] || 't-gris'}">${esc(v.replace('_', ' '))}</span>` : '';

    // ------------------------------------------------------------ iconos
    const ICONOS = {
        inicio: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>',
        clientes: '<path d="M3 21V7l6-4 6 4v14"/><path d="M15 11h6v10h-6M7 9h4M7 13h4M7 17h4"/>',
        vehiculos: '<path d="M3 16V9l2-4h9l4 4h3v7"/><circle cx="7.5" cy="16.5" r="2.5"/><circle cx="17.5" cy="16.5" r="2.5"/>',
        contratos: '<path d="M14 3H6v18h12V7z"/><path d="M14 3v4h4M9 12h6M9 16h6"/>',
        pagos: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>',
        mantenimientos: '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z"/>',
        usuarios: '<circle cx="9" cy="8" r="4"/><path d="M2 21c0-4 3-6 7-6s7 2 7 6M17 11a3 3 0 1 0 0-6M22 21c0-3-2-5-4-5.5"/>',
        dinero: '<path d="M12 2v20M17 6H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
        alerta: '<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18h.01"/>',
    };
    const icono = (n) => `<svg viewBox="0 0 24 24">${ICONOS[n] || ''}</svg>`;

    // ------------------------------------------------------------ catalogos
    const OPC = {
        estadoRegistro: ['ACTIVO', 'INACTIVO'],
        estadoVehiculo: ['DISPONIBLE', 'MANTENIMIENTO', 'INACTIVO'],
        estadoContrato: ['PENDIENTE', 'ACTIVO', 'FINALIZADO', 'CANCELADO'],
        estadoPago: ['PENDIENTE', 'CONFIRMADO', 'ANULADO'],
        tipoComprobante: ['FACTURA', 'BOLETA', 'RECIBO'],
        metodoPago: ['EFECTIVO', 'TRANSFERENCIA', 'DEPOSITO', 'CHEQUE', 'TARJETA'],
        tipoMant: ['PREVENTIVO', 'CORRECTIVO', 'REVISION'],
        estadoMant: ['PROGRAMADO', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO'],
        roles: ['ADMIN', 'OPERADOR', 'PAGOS', 'GERENCIA'],
        sectores: ['mineria', 'construccion', 'agricultura', 'transporte', 'otros'],
        tiposVehiculo: ['4x4 pickup', '4x4 suv', '4x4 camion', '4x4 van'],
        combustible: ['', 'LLENO', '3/4', '1/2', '1/4', 'VACIO'],
    };
    const lista = (arr) => arr.map((v) => ({ valor: v, texto: v ? v.replace('_', ' ') : '—' }));

    // Cada recurso define columnas de la tabla y campos del formulario
    const RECURSOS = {
        clientes: {
            titulo: 'Clientes', singular: 'cliente', ruta: '/api/clientes',
            filtros: [{ tipo: 'buscar', param: 'buscar', placeholder: 'Buscar por razón social' }, { tipo: 'select', param: 'estado', opciones: OPC.estadoRegistro, todos: 'Todos los estados' }],
            columnas: [
                { t: 'Razón social', v: (c) => `<strong>${esc(c.razonSocial)}</strong><span class="sub">RUC ${esc(c.ruc)}</span>` },
                { t: 'Sector', v: (c) => esc(c.sector || '—') },
                { t: 'Contacto', v: (c) => `${esc(c.contactoNombre || '—')}<span class="sub">${esc(c.telefono || '')}</span>` },
                { t: 'Email', v: (c) => esc(c.email || '—') },
                { t: 'Estado', v: (c) => badge(c.estado) },
            ],
            campos: [
                { n: 'razonSocial', l: 'Razón social', req: true, ancho: true },
                { n: 'ruc', l: 'RUC', req: true, ph: '20XXXXXXXXX' },
                { n: 'sector', l: 'Sector', tipo: 'select', opciones: lista(['', ...OPC.sectores]) },
                { n: 'direccion', l: 'Dirección', ancho: true },
                { n: 'telefono', l: 'Teléfono' },
                { n: 'email', l: 'Email', tipo: 'email' },
                { n: 'contactoNombre', l: 'Nombre de contacto' },
                { n: 'contactoCargo', l: 'Cargo del contacto' },
                { n: 'contactoTelefono', l: 'Teléfono de contacto' },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoRegistro) },
                { n: 'observaciones', l: 'Observaciones', tipo: 'textarea', ancho: true },
            ],
        },
        vehiculos: {
            titulo: 'Vehículos', singular: 'vehículo', ruta: '/api/vehiculos',
            filtros: [{ tipo: 'select', param: 'estado', opciones: ['DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO', 'INACTIVO'], todos: 'Todos los estados' }],
            columnas: [
                { t: 'Placa', v: (v) => `<strong>${esc(v.placa)}</strong>` },
                { t: 'Vehículo', v: (v) => `${esc(v.marca)} ${esc(v.modelo)}<span class="sub">${esc(v.anio)} · ${esc(v.tipo || '')}</span>` },
                { t: 'Kilometraje', v: (v) => Number(v.kilometraje).toLocaleString('es-PE') + ' km', num: true },
                { t: 'Tarifa diaria', v: (v) => soles(v.tarifaDiaria), num: true },
                { t: 'SOAT', v: (v) => fecha(v.fechaSoat) },
                { t: 'Estado', v: (v) => badge(v.estado) },
            ],
            campos: [
                { n: 'placa', l: 'Placa', req: true, ph: 'ABC-123', fijoEnEdicion: true, mayus: true },
                { n: 'marca', l: 'Marca', req: true, fijoEnEdicion: true },
                { n: 'modelo', l: 'Modelo', req: true, fijoEnEdicion: true },
                { n: 'anio', l: 'Año', tipo: 'number', req: true },
                { n: 'tipo', l: 'Tipo', tipo: 'select', opciones: lista(OPC.tiposVehiculo) },
                { n: 'color', l: 'Color' },
                { n: 'kilometraje', l: 'Kilometraje', tipo: 'number' },
                { n: 'tarifaDiaria', l: 'Tarifa diaria (S/)', tipo: 'number', paso: '0.01', req: true },
                { n: 'capacidadPasajeros', l: 'Pasajeros', tipo: 'number' },
                { n: 'capacidadCarga', l: 'Carga (t)', tipo: 'number', paso: '0.01' },
                { n: 'fechaSoat', l: 'Vencimiento SOAT', tipo: 'date' },
                { n: 'fechaRevisionTecnica', l: 'Revisión técnica', tipo: 'date' },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoVehiculo), soloEdicion: true },
                { n: 'observaciones', l: 'Observaciones', tipo: 'textarea', ancho: true },
            ],
            notaEdicion: 'Placa, marca y modelo no se modifican después del registro.',
        },
        contratos: {
            titulo: 'Contratos', singular: 'contrato', ruta: '/api/contratos',
            filtros: [{ tipo: 'select', param: 'estado', opciones: OPC.estadoContrato, todos: 'Todos los estados' }],
            columnas: [
                { t: 'N.°', v: (c) => `<strong>${esc(c.numeroContrato)}</strong>` },
                { t: 'Cliente', v: (c) => esc(c.clienteRazonSocial) },
                { t: 'Vehículo', v: (c) => `${esc(c.vehiculoPlaca)}<span class="sub">${esc(c.vehiculoDescripcion)}</span>` },
                { t: 'Periodo', v: (c) => `${fecha(c.fechaInicio)} – ${fecha(c.fechaFin)}<span class="sub">${c.diasAlquiler} día(s)</span>` },
                { t: 'Total', v: (c) => soles(c.total), num: true },
                { t: 'Estado', v: (c) => badge(c.estado) },
            ],
            campos: [
                { n: 'idCliente', l: 'Cliente', tipo: 'select', req: true, ancho: true, cargar: async () => (await api('GET', '/api/clientes?estado=ACTIVO')).map((c) => ({ valor: c.id, texto: `${c.razonSocial} (RUC ${c.ruc})` })) },
                { n: 'idVehiculo', l: 'Vehículo', tipo: 'select', req: true, ancho: true, cargar: async (reg) => (await api('GET', '/api/vehiculos')).filter((v) => v.estado === 'DISPONIBLE' || (reg && v.id === reg.idVehiculo)).map((v) => ({ valor: v.id, texto: `${v.placa} · ${v.marca} ${v.modelo} · ${soles(v.tarifaDiaria)}/día`, tarifa: v.tarifaDiaria, km: v.kilometraje })) },
                { n: 'fechaInicio', l: 'Fecha de inicio', tipo: 'date', req: true },
                { n: 'fechaFin', l: 'Fecha de fin', tipo: 'date', req: true },
                { n: 'garantia', l: 'Garantía (S/)', tipo: 'number', paso: '0.01' },
                { n: 'kilometrajeInicial', l: 'Km inicial', tipo: 'number' },
                { n: 'combustibleInicial', l: 'Combustible inicial', tipo: 'select', opciones: lista(OPC.combustible) },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoContrato), defecto: () => 'ACTIVO' },
                { n: 'lugarEntrega', l: 'Lugar de entrega' },
                { n: 'lugarDevolucion', l: 'Lugar de devolución' },
                { n: 'kilometrajeFinal', l: 'Km final (para finalizar)', tipo: 'number', soloEdicion: true },
                { n: 'combustibleFinal', l: 'Combustible final', tipo: 'select', opciones: lista(OPC.combustible), soloEdicion: true },
                { n: 'observaciones', l: 'Observaciones', tipo: 'textarea', ancho: true },
            ],
            alCambiar: previsualizarContrato,
            editable: (c) => c.estado === 'PENDIENTE' || c.estado === 'ACTIVO',
        },
        pagos: {
            titulo: 'Pagos', singular: 'pago', ruta: '/api/pagos',
            columnas: [
                { t: 'Comprobante', v: (p) => `<strong>${esc(p.numeroComprobante)}</strong><span class="sub">${esc(p.tipoComprobante)}</span>` },
                { t: 'Contrato', v: (p) => `${esc(p.numeroContrato)}<span class="sub">${esc(p.clienteRazonSocial)}</span>` },
                { t: 'Fecha', v: (p) => fecha(p.fechaPago) },
                { t: 'Método', v: (p) => `${esc(p.metodoPago)}<span class="sub">${esc(p.banco || '')}</span>` },
                { t: 'Monto', v: (p) => soles(p.monto), num: true },
                { t: 'Estado', v: (p) => badge(p.estado) },
            ],
            campos: [
                { n: 'idContrato', l: 'Contrato', tipo: 'select', req: true, ancho: true, cargar: async () => (await api('GET', '/api/contratos')).filter((c) => c.estado !== 'CANCELADO').map((c) => ({ valor: c.id, texto: `${c.numeroContrato} · ${c.clienteRazonSocial} · total ${soles(c.total)}` })) },
                { n: 'numeroComprobante', l: 'N.° de comprobante', req: true, ph: 'F001-123', mayus: true },
                { n: 'tipoComprobante', l: 'Tipo', tipo: 'select', req: true, opciones: lista(OPC.tipoComprobante) },
                { n: 'fechaPago', l: 'Fecha de pago', tipo: 'date', req: true, defecto: hoyISO },
                { n: 'monto', l: 'Monto (S/)', tipo: 'number', paso: '0.01', req: true },
                { n: 'metodoPago', l: 'Método', tipo: 'select', req: true, opciones: lista(OPC.metodoPago), defecto: () => 'TRANSFERENCIA' },
                { n: 'banco', l: 'Banco' },
                { n: 'numeroOperacion', l: 'N.° de operación' },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoPago), defecto: () => 'CONFIRMADO' },
                { n: 'observaciones', l: 'Observaciones', tipo: 'textarea', ancho: true },
            ],
            editable: (p) => p.estado !== 'ANULADO',
        },
        mantenimientos: {
            titulo: 'Mantenimientos', singular: 'mantenimiento', ruta: '/api/mantenimientos',
            columnas: [
                { t: 'Vehículo', v: (m) => `<strong>${esc(m.vehiculoPlaca)}</strong><span class="sub">${esc(m.vehiculoDescripcion)}</span>` },
                { t: 'Tipo', v: (m) => esc(m.tipo) },
                { t: 'Descripción', v: (m) => `${esc(m.descripcion)}<span class="sub">${esc(m.taller || '')}</span>` },
                { t: 'Inicio', v: (m) => fecha(m.fechaInicio) },
                { t: 'Costo', v: (m) => soles(m.costo), num: true },
                { t: 'Estado', v: (m) => badge(m.estado) },
            ],
            campos: [
                { n: 'idVehiculo', l: 'Vehículo', tipo: 'select', req: true, ancho: true, cargar: async (reg) => (await api('GET', '/api/vehiculos')).filter((v) => v.estado !== 'ALQUILADO' || (reg && v.id === reg.idVehiculo)).map((v) => ({ valor: v.id, texto: `${v.placa} · ${v.marca} ${v.modelo} · ${v.estado}` })) },
                { n: 'tipo', l: 'Tipo', tipo: 'select', req: true, opciones: lista(OPC.tipoMant) },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoMant) },
                { n: 'fechaInicio', l: 'Fecha de inicio', tipo: 'date', req: true, defecto: hoyISO },
                { n: 'fechaFin', l: 'Fecha de fin', tipo: 'date' },
                { n: 'kilometraje', l: 'Kilometraje', tipo: 'number' },
                { n: 'costo', l: 'Costo (S/)', tipo: 'number', paso: '0.01' },
                { n: 'taller', l: 'Taller' },
                { n: 'proximoMantenimiento', l: 'Próximo mantenimiento', tipo: 'date' },
                { n: 'descripcion', l: 'Descripción', tipo: 'textarea', req: true, ancho: true },
                { n: 'observaciones', l: 'Observaciones', tipo: 'textarea', ancho: true },
            ],
            editable: (m) => m.estado === 'PROGRAMADO' || m.estado === 'EN_PROCESO',
        },
        usuarios: {
            titulo: 'Usuarios', singular: 'usuario', ruta: '/api/usuarios', soloAdmin: true,
            columnas: [
                { t: 'Nombre', v: (u) => `<strong>${esc(u.nombreCompleto)}</strong><span class="sub">${esc(u.email)}</span>` },
                { t: 'Rol', v: (u) => badge(u.rol) },
                { t: 'Teléfono', v: (u) => esc(u.telefono || '—') },
                { t: 'Última conexión', v: (u) => u.ultimaConexion ? fecha(u.ultimaConexion) + ' ' + u.ultimaConexion.slice(11, 16) : 'Nunca' },
                { t: 'Estado', v: (u) => badge(u.estado) },
            ],
            campos: [
                { n: 'nombreCompleto', l: 'Nombre completo', req: true, ancho: true },
                { n: 'email', l: 'Email', tipo: 'email', req: true },
                { n: 'password', l: 'Contraseña', tipo: 'password', ph: 'Mínimo 8 caracteres' },
                { n: 'rol', l: 'Rol', tipo: 'select', req: true, opciones: lista(OPC.roles) },
                { n: 'telefono', l: 'Teléfono' },
                { n: 'estado', l: 'Estado', tipo: 'select', opciones: lista(OPC.estadoRegistro) },
            ],
            notaEdicion: 'Deja la contraseña vacía para mantener la actual. Se guarda cifrada con BCrypt.',
        },
    };

    // ------------------------------------------------------------ login
    const HERO = [
        ['Tu flota 4x4 bajo control, desde cualquier lugar.', 'Disponibilidad en tiempo real.', 'Sabe al instante qué camioneta está libre, alquilada o en taller.'],
        ['Contratos sin cruces de fechas.', 'El sistema valida por ti.', 'Una misma unidad nunca se compromete en dos periodos a la vez.'],
        ['Importes correctos en cada contrato.', 'IGV y totales automáticos.', 'Días, subtotal, IGV del 18 % y total calculados en el servidor.'],
        ['Mantenimiento al día, flota segura.', 'Cuidar la unidad también es prevenir.', 'Registra mantenimientos, kilometraje y vencimientos de SOAT.'],
    ];
    let heroIdx = 0, heroTimer = null;
    function pintarHero() {
        const [t, s, d] = HERO[heroIdx];
        $('#hero-titulo').textContent = t; $('#hero-sub').textContent = s; $('#hero-det').textContent = d;
        $('#hero-puntos').innerHTML = HERO.map((_, i) => `<span class="${i === heroIdx ? 'activo' : ''}"></span>`).join('');
    }
    function iniciarHero() {
        pintarHero();
        clearInterval(heroTimer);
        heroTimer = setInterval(() => { heroIdx = (heroIdx + 1) % HERO.length; pintarHero(); }, 5000);
    }

    function mostrarLogin(msg) {
        $('#vista-app').classList.add('oculto');
        $('#vista-login').classList.remove('oculto');
        $('#login-error').classList.toggle('oculto', !msg);
        $('#login-error').textContent = msg || '';
        iniciarHero();
    }

    $('#btn-mostrar').addEventListener('click', () => {
        const i = $('#login-password');
        i.type = i.type === 'password' ? 'text' : 'password';
        $('#btn-mostrar').textContent = i.type === 'password' ? 'Mostrar' : 'Ocultar';
    });
    document.querySelectorAll('[data-demo]').forEach((b) => b.addEventListener('click', () => {
        $('#login-email').value = b.dataset.demo;
        $('#login-password').value = 'Chd12345*';
    }));

    $('#form-login').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = $('#login-email').value.trim();
        const password = $('#login-password').value;
        const err = $('#login-error');
        if (!email || !password) {
            err.textContent = 'Completa el correo y la contraseña.'; err.classList.remove('oculto'); return;
        }
        const btn = $('#btn-login');
        btn.disabled = true; btn.textContent = 'Entrando...';
        err.classList.add('oculto');
        // si el servidor gratuito esta dormido la respuesta tarda; avisamos al usuario
        const aviso = setTimeout(() => $('#login-aviso').classList.remove('oculto'), 3000);
        try {
            const r = await api('POST', '/api/auth/login', { email, password });
            sesion.guardar(r.token, r.usuario);
            $('#login-password').value = '';
            entrarApp();
        } catch (ex) {
            err.textContent = ex.status === 401 ? 'Correo o contraseña incorrectos.'
                : ex.status === 400 ? 'Revisa el formato del correo.' : 'No se pudo conectar con el servidor. Intenta de nuevo.';
            err.classList.remove('oculto');
        } finally {
            clearTimeout(aviso);
            $('#login-aviso').classList.add('oculto');
            btn.disabled = false; btn.textContent = 'Ingresar';
        }
    });

    $('#btn-salir').addEventListener('click', () => { sesion.limpiar(); location.hash = ''; mostrarLogin(); });

    // ------------------------------------------------------------ app y navegacion
    function entrarApp() {
        clearInterval(heroTimer);
        const u = sesion.usr();
        $('#vista-login').classList.add('oculto');
        $('#vista-app').classList.remove('oculto');
        $('#usuario-nombre').textContent = u.nombreCompleto;
        $('#usuario-rol').textContent = u.rol;
        $('#usuario-avatar').textContent = (u.nombreCompleto || '?').trim().charAt(0).toUpperCase();
        const items = [['inicio', 'Inicio'], ...Object.entries(RECURSOS).filter(([, r]) => !r.soloAdmin || rol() === 'ADMIN').map(([k, r]) => [k, r.titulo])];
        $('#menu').innerHTML = items.map(([k, t]) => `<a href="#${k}" data-vista="${k}">${icono(k)}<span>${t}</span></a>`).join('');
        navegar();
    }

    function navegar() {
        if (!sesion.tok()) return;
        let vista = location.hash.slice(1) || 'inicio';
        if (vista !== 'inicio' && (!RECURSOS[vista] || (RECURSOS[vista].soloAdmin && rol() !== 'ADMIN'))) vista = 'inicio';
        document.querySelectorAll('#menu a').forEach((a) => a.classList.toggle('activo', a.dataset.vista === vista));
        $('#sidebar').classList.remove('abierto');
        $('#titulo-vista').textContent = vista === 'inicio' ? 'Inicio' : RECURSOS[vista].titulo;
        if (vista === 'inicio') vistaInicio(); else vistaRecurso(vista);
    }
    window.addEventListener('hashchange', navegar);
    $('#btn-menu').addEventListener('click', () => $('#sidebar').classList.toggle('abierto'));

    const cargando = () => { $('#contenido').innerHTML = '<div class="vacio">Cargando...</div>'; };

    // ------------------------------------------------------------ inicio (dashboard)
    async function vistaInicio() {
        cargando();
        try {
            const pedirPagos = ['ADMIN', 'PAGOS', 'GERENCIA', 'OPERADOR'].includes(rol());
            const [vehiculos, contratos, clientes, pagos, mants] = await Promise.all([
                api('GET', '/api/vehiculos'), api('GET', '/api/contratos'), api('GET', '/api/clientes'),
                pedirPagos ? api('GET', '/api/pagos') : [], api('GET', '/api/mantenimientos'),
            ]);
            const cuenta = (est) => vehiculos.filter((v) => v.estado === est).length;
            const activos = contratos.filter((c) => c.estado === 'ACTIVO');
            const cobrado = pagos.filter((p) => p.estado === 'CONFIRMADO').reduce((s, p) => s + Number(p.monto), 0);
            const limite = new Date(Date.now() + 60 * 864e5).toISOString().slice(0, 10);
            const vencen = vehiculos.flatMap((v) => [
                v.fechaSoat && v.fechaSoat <= limite ? { v, doc: 'SOAT', f: v.fechaSoat } : null,
                v.fechaRevisionTecnica && v.fechaRevisionTecnica <= limite ? { v, doc: 'Revisión técnica', f: v.fechaRevisionTecnica } : null,
            ].filter(Boolean)).sort((a, b) => a.f.localeCompare(b.f));
            const abiertos = mants.filter((m) => m.estado === 'PROGRAMADO' || m.estado === 'EN_PROCESO');

            const kpi = (ic, tono, valor, label) => `<div class="kpi"><div class="kpi-icono ${tono}">${icono(ic)}</div><div><div class="kpi-valor">${valor}</div><div class="kpi-label">${label}</div></div></div>`;
            $('#contenido').innerHTML = `
                <div class="kpis">
                    ${kpi('vehiculos', 't-info', vehiculos.length, 'Vehículos en la flota')}
                    ${kpi('vehiculos', 't-ok', cuenta('DISPONIBLE'), 'Disponibles ahora')}
                    ${kpi('contratos', 't-info', cuenta('ALQUILADO'), 'Alquilados')}
                    ${kpi('mantenimientos', 't-warn', cuenta('MANTENIMIENTO'), 'En mantenimiento')}
                    ${kpi('clientes', 't-gris', clientes.length, 'Clientes registrados')}
                    ${kpi('dinero', 't-ok', soles(cobrado), 'Pagos confirmados')}
                </div>
                <div class="grid-2">
                    <div class="panel">
                        <h2>Contratos activos (${activos.length})</h2>
                        <div class="tabla-wrap">${activos.length ? `<table><thead><tr><th>N.°</th><th>Cliente</th><th>Vehículo</th><th>Hasta</th><th style="text-align:right">Total</th></tr></thead><tbody>
                            ${activos.slice(0, 8).map((c) => `<tr><td><strong>${esc(c.numeroContrato)}</strong></td><td>${esc(c.clienteRazonSocial)}</td><td>${esc(c.vehiculoPlaca)}</td><td>${fecha(c.fechaFin)}</td><td class="num">${soles(c.total)}</td></tr>`).join('')}
                        </tbody></table>` : '<div class="vacio">No hay contratos activos.</div>'}</div>
                    </div>
                    <div class="panel">
                        <h2>Alertas de la flota</h2>
                        <div class="tabla-wrap">${vencen.length || abiertos.length ? `<table><tbody>
                            ${vencen.map((x) => `<tr><td><span class="badge t-warn">${x.doc}</span></td><td><strong>${esc(x.v.placa)}</strong><span class="sub">vence ${fecha(x.f)}</span></td></tr>`).join('')}
                            ${abiertos.map((m) => `<tr><td>${badge(m.estado)}</td><td><strong>${esc(m.vehiculoPlaca)}</strong><span class="sub">${esc(m.descripcion)}</span></td></tr>`).join('')}
                        </tbody></table>` : '<div class="vacio">Sin vencimientos en los próximos 60 días ni mantenimientos abiertos.</div>'}</div>
                    </div>
                </div>`;
        } catch (ex) {
            if (ex.status !== 401) $('#contenido').innerHTML = `<div class="alerta-error">${esc(ex.message)}</div>`;
        }
    }

    // ------------------------------------------------------------ vistas CRUD
    const filtrosActivos = {};
    async function vistaRecurso(clave) {
        const R = RECURSOS[clave];
        const f = filtrosActivos[clave] || (filtrosActivos[clave] = {});
        const barra = (R.filtros || []).map((x) => x.tipo === 'buscar'
            ? `<input type="search" data-filtro="${x.param}" placeholder="${esc(x.placeholder)}" value="${esc(f[x.param] || '')}">`
            : `<select data-filtro="${x.param}"><option value="">${esc(x.todos)}</option>${x.opciones.map((o) => `<option ${f[x.param] === o ? 'selected' : ''}>${o}</option>`).join('')}</select>`).join('');
        $('#contenido').innerHTML = `
            <div class="barra">${barra}<span class="espacio"></span>
                ${puede.escribir(clave) ? `<button class="btn btn-primario" id="btn-nuevo">+ Nuevo ${R.singular}</button>` : ''}
            </div>
            <div class="tabla-wrap" id="tabla"><div class="vacio">Cargando...</div></div>`;

        document.querySelectorAll('[data-filtro]').forEach((el) => {
            const ev = el.tagName === 'SELECT' ? 'change' : 'input';
            let t;
            el.addEventListener(ev, () => { clearTimeout(t); t = setTimeout(() => { f[el.dataset.filtro] = el.value; cargarTabla(clave); }, 250); });
        });
        const nuevo = $('#btn-nuevo');
        if (nuevo) nuevo.addEventListener('click', () => abrirFormulario(clave, null));
        cargarTabla(clave);
    }

    async function cargarTabla(clave) {
        const R = RECURSOS[clave];
        const f = filtrosActivos[clave] || {};
        const qs = new URLSearchParams(Object.entries(f).filter(([, v]) => v)).toString();
        try {
            const datos = await api('GET', R.ruta + (qs ? '?' + qs : ''));
            const acciones = puede.escribir(clave) || puede.eliminar();
            $('#tabla').innerHTML = datos.length ? `<table>
                <thead><tr>${R.columnas.map((c) => `<th ${c.num ? 'style="text-align:right"' : ''}>${c.t}</th>`).join('')}${acciones ? '<th></th>' : ''}</tr></thead>
                <tbody>${datos.map((d) => `<tr>${R.columnas.map((c) => `<td class="${c.num ? 'num' : ''}">${c.v(d)}</td>`).join('')}
                    ${acciones ? `<td class="acciones">
                        ${puede.escribir(clave) && (!R.editable || R.editable(d)) ? `<button class="btn btn-secundario btn-chico" data-editar="${d.id}">Editar</button>` : ''}
                        ${puede.eliminar() ? `<button class="btn btn-peligro btn-chico" data-eliminar="${d.id}">Eliminar</button>` : ''}
                    </td>` : ''}</tr>`).join('')}
                </tbody></table>` : `<div class="vacio">No hay ${R.titulo.toLowerCase()} para mostrar.</div>`;
            $('#tabla').querySelectorAll('[data-editar]').forEach((b) => b.addEventListener('click', () =>
                abrirFormulario(clave, datos.find((d) => String(d.id) === b.dataset.editar))));
            $('#tabla').querySelectorAll('[data-eliminar]').forEach((b) => b.addEventListener('click', () =>
                eliminar(clave, datos.find((d) => String(d.id) === b.dataset.eliminar))));
        } catch (ex) {
            if (ex.status !== 401) $('#tabla').innerHTML = `<div class="alerta-error">${esc(ex.message)}</div>`;
        }
    }

    async function eliminar(clave, reg) {
        const R = RECURSOS[clave];
        if (!confirm(`¿Eliminar este ${R.singular}? Esta acción no se puede deshacer.`)) return;
        try {
            await api('DELETE', `${R.ruta}/${reg.id}`);
            toast(`${R.singular.charAt(0).toUpperCase() + R.singular.slice(1)} eliminado.`, 'ok');
            cargarTabla(clave);
        } catch (ex) {
            if (ex.status !== 401) toast(ex.message, 'error');
        }
    }

    // ------------------------------------------------------------ formulario modal
    let formActual = null;
    async function abrirFormulario(clave, reg) {
        const R = RECURSOS[clave];
        const edicion = !!reg;
        formActual = { clave, reg };
        $('#modal-titulo').textContent = (edicion ? 'Editar ' : 'Nuevo ') + R.singular;
        $('#modal-error').classList.add('oculto');
        $('#modal-extra').innerHTML = edicion && R.notaEdicion ? `<p class="muted">${esc(R.notaEdicion)}</p>` : '';
        $('#modal-cuerpo').innerHTML = '<div class="vacio ancho">Cargando...</div>';
        $('#modal').classList.remove('oculto');

        const campos = R.campos.filter((c) => !c.soloEdicion || edicion);
        const opciones = {};
        try {
            for (const c of campos) if (c.cargar) opciones[c.n] = await c.cargar(reg);
        } catch (ex) {
            $('#modal-cuerpo').innerHTML = `<div class="alerta-error ancho">${esc(ex.message)}</div>`;
            return;
        }
        $('#modal-cuerpo').innerHTML = campos.map((c) => {
            let valor = reg ? reg[c.n] : (c.defecto ? c.defecto() : '');
            if (c.tipo === 'password') valor = '';
            const attrs = `id="f-${c.n}" name="${c.n}" ${c.req && !(c.tipo === 'password' && edicion) ? 'required' : ''} ${edicion && c.fijoEnEdicion ? 'disabled' : ''} ${c.ph ? `placeholder="${esc(c.ph)}"` : ''}`;
            let input;
            if (c.tipo === 'select') {
                const ops = opciones[c.n] || c.opciones;
                const vacio = c.cargar ? `<option value="">Selecciona...</option>` : '';
                input = `<select ${attrs}>${vacio}${ops.map((o) => `<option value="${esc(o.valor)}" ${o.tarifa != null ? `data-tarifa="${o.tarifa}" data-km="${o.km}"` : ''} ${String(o.valor) === String(valor ?? '') ? 'selected' : ''}>${esc(o.texto)}</option>`).join('')}</select>`;
            } else if (c.tipo === 'textarea') {
                input = `<textarea ${attrs}>${esc(valor)}</textarea>`;
            } else {
                input = `<input type="${c.tipo || 'text'}" ${c.paso ? `step="${c.paso}"` : ''} value="${esc(valor)}" ${attrs}>`;
            }
            return `<div class="${c.ancho ? 'ancho' : ''}" data-campo="${c.n}"><label for="f-${c.n}">${esc(c.l)}${c.req ? ' *' : ''}</label>${input}</div>`;
        }).join('');

        if (clave === 'usuarios' && !edicion) $('#f-password').required = true;
        if (R.alCambiar) {
            $('#modal-cuerpo').querySelectorAll('input, select').forEach((el) => el.addEventListener('input', () => R.alCambiar(edicion)));
            R.alCambiar(edicion);
        }
        const primero = $('#modal-cuerpo').querySelector('input:not([disabled]), select:not([disabled])');
        if (primero) primero.focus();
    }

    function previsualizarContrato(edicion) {
        const sel = $('#f-idVehiculo');
        const op = sel && sel.selectedOptions[0];
        const ini = $('#f-fechaInicio').value, fin = $('#f-fechaFin').value;
        let tarifa = op && op.dataset.tarifa ? Number(op.dataset.tarifa) : null;
        if (edicion && formActual.reg && String(formActual.reg.idVehiculo) === sel.value) tarifa = Number(formActual.reg.tarifaDiaria);
        if (!edicion && op && op.dataset.km && !$('#f-kilometrajeInicial').value) $('#f-kilometrajeInicial').value = op.dataset.km;
        let html = '';
        if (tarifa && ini && fin && fin >= ini) {
            const dias = Math.max(1, Math.round((new Date(fin) - new Date(ini)) / 864e5));
            const sub = Math.round(tarifa * dias * 100) / 100;
            const igv = Math.round(sub * 18) / 100;
            html = `<div class="resumen">
                <div><span>Días</span><strong>${dias}</strong></div>
                <div><span>Subtotal</span><strong>${soles(sub)}</strong></div>
                <div><span>IGV 18 %</span><strong>${soles(igv)}</strong></div>
                <div><span>Total</span><strong>${soles(sub + igv)}</strong></div>
            </div><p class="muted">Vista previa. El importe definitivo lo calcula el servidor con la tarifa del vehículo.</p>`;
        } else if (ini && fin && fin < ini) {
            html = '<div class="alerta-error">La fecha de fin no puede ser anterior a la de inicio.</div>';
        }
        $('#modal-extra').innerHTML = html;
    }

    function leerFormulario() {
        const R = RECURSOS[formActual.clave];
        const datos = {};
        R.campos.forEach((c) => {
            const el = $('#f-' + c.n);
            if (!el) {
                if (formActual.reg && formActual.reg[c.n] !== undefined && c.n !== 'password') datos[c.n] = formActual.reg[c.n];
                return;
            }
            let v = el.value.trim();
            if (c.mayus) v = v.toUpperCase();
            if (v === '') { datos[c.n] = null; return; }
            datos[c.n] = c.tipo === 'number' || /^id[A-Z]/.test(c.n) ? Number(v) : v;
        });
        if (formActual.clave === 'usuarios' && !datos.password) delete datos.password;
        return datos;
    }

    function marcarErrores(errores) {
        $('#modal-cuerpo').querySelectorAll('.msg-campo').forEach((m) => m.remove());
        $('#modal-cuerpo').querySelectorAll('.campo-error').forEach((m) => m.classList.remove('campo-error'));
        Object.entries(errores).forEach(([campo, msg]) => {
            const cont = $(`[data-campo="${campo}"]`);
            if (!cont) return;
            cont.classList.add('campo-error');
            cont.insertAdjacentHTML('beforeend', `<div class="msg-campo">${esc(msg)}</div>`);
        });
    }

    $('#modal-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const { clave, reg } = formActual;
        const R = RECURSOS[clave];
        const faltan = [...$('#modal-cuerpo').querySelectorAll('[required]')].filter((el) => !el.value.trim());
        if (faltan.length) {
            marcarErrores(Object.fromEntries(faltan.map((el) => [el.name, 'Este campo es obligatorio'])));
            return;
        }
        const btn = $('#modal-guardar');
        btn.disabled = true; btn.textContent = 'Guardando...';
        try {
            await api(reg ? 'PUT' : 'POST', reg ? `${R.ruta}/${reg.id}` : R.ruta, leerFormulario());
            cerrarModal();
            toast(`${R.singular.charAt(0).toUpperCase() + R.singular.slice(1)} ${reg ? 'actualizado' : 'registrado'} correctamente.`, 'ok');
            cargarTabla(clave);
        } catch (ex) {
            if (ex.status === 401) { cerrarModal(); return; }
            marcarErrores(ex.errores);
            $('#modal-error').textContent = ex.message;
            $('#modal-error').classList.remove('oculto');
        } finally {
            btn.disabled = false; btn.textContent = 'Guardar';
        }
    });

    function cerrarModal() { $('#modal').classList.add('oculto'); formActual = null; }
    $('#modal-cerrar').addEventListener('click', cerrarModal);
    $('#modal-cancelar').addEventListener('click', cerrarModal);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !$('#modal').classList.contains('oculto')) cerrarModal(); });

    // ------------------------------------------------------------ arranque
    if (sesion.tok() && sesion.usr()) entrarApp(); else mostrarLogin();
})();
