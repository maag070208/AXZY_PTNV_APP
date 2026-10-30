package com.axzydev.puertonuevoapp.feature.langostin.model

import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.PermissionKeys as P
import com.axzydev.puertonuevoapp.core.session.SessionUser

/**
 * Acción que Langostín ofrece dentro de una pantalla.
 *
 * - [Answer] es una pregunta que contesta en el hilo.
 * - [GoTo] navega a otra pantalla cuando la persona tiene el permiso; el API
 *   vuelve a validar, aquí solo se oculta.
 */
sealed interface LangostinAction {
    /** Pregunta que Langostín contesta en el hilo. */
    data class Answer(val label: String, val answer: String) : LangostinAction

    /** Lleva a otra pantalla (solo si la persona tiene el permiso). */
    data class GoTo(
        val label: String,
        val answer: String,
        val to: Screen,
        val permission: String? = null,
    ) : LangostinAction
}

/** Visibilidad por permisos de la sesión. */
fun LangostinAction.isVisibleFor(user: SessionUser?): Boolean = when (this) {
    is LangostinAction.Answer -> true
    is LangostinAction.GoTo -> permission == null || user?.can(permission) == true
}

/** Lo que Langostín sabe de una pantalla. */
data class LangostinScreen(
    val id: String,
    val title: String,
    val explain: String,
    val match: (Screen) -> Boolean,
    val actions: List<LangostinAction>,
)

/**
 * Registro de ayuda contextual de la app, portado del registro de la web
 * (`web/src/widgets/assistant/model/helpRegistry.ts`).
 *
 * Reglas:
 * - Langostín **solo ofrece lo registrado aquí**: nunca inventa pantallas,
 *   rutas ni permisos.
 * - `match` se evalúa en orden y la primera que casa gana; la última entrada es
 *   `default` y siempre casa.
 * - Toda `GoTo.to` es una `Screen` real de la app y todo `permission` es una
 *   clave de `PermissionKeys`.
 * - Las pantallas de la web que la app no tiene (Horarios, Nómina, Cocina,
 *   reloj checador, expedientes) se explican como `Answer`; no se navega a
 *   rutas que no existen.
 */
object LangostinHelp {

    // ── Específicas: detalles y formularios (antes que las listas) ──────────

    private val ticketDetail = LangostinScreen(
        id = "tickets.detail",
        title = "Detalle del ticket",
        explain = "Aquí ves el ticket completo: estado, descripción, comentarios, historial y tareas.",
        match = { it is Screen.TicketDetail },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo cambio el estado?",
                answer = "Usa el selector «Cambiar estado». Abierto y en seguimiento piden permiso de edición; cerrado pide permiso de cierre.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo agrego un comentario?",
                answer = "Escribe en el campo de Comentarios y toca enviar. Queda en el historial con tu nombre.",
            ),
            LangostinAction.Answer(
                label = "¿Dónde veo las tareas?",
                answer = "Toca «Tareas del ticket» para abrir el tablero con las tareas de este ticket.",
            ),
            LangostinAction.GoTo(
                label = "Ver la lista de tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ir al tablero",
                answer = "Te llevo al tablero de tareas.",
                to = Screen.TicketsKanban(),
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val ticketEdit = LangostinScreen(
        id = "tickets.edit",
        title = "Editar ticket",
        explain = "Corrige título, descripción, prioridad y categoría. El estado se cambia desde el detalle.",
        match = { it is Screen.EditTicket },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué puedo cambiar aquí?",
                answer = "Título, descripción, prioridad y categoría. El estado y las tareas se manejan en el detalle.",
            ),
            LangostinAction.Answer(
                label = "¿Por qué no me deja guardar?",
                answer = "Título y descripción son obligatorios: si falta uno, el botón no guarda.",
            ),
            LangostinAction.Answer(
                label = "¿Quién puede editar?",
                answer = "Quien tenga el permiso de edición con el alcance que corresponde: lo propio, su área o todo.",
            ),
            LangostinAction.GoTo(
                label = "Volver a la lista",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val ticketNew = LangostinScreen(
        id = "tickets.new",
        title = "Nuevo ticket",
        explain = "Alta de un ticket: título, descripción, prioridad y categoría.",
        match = { it is Screen.NewTicket },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es obligatorio?",
                answer = "Título y descripción. La prioridad viene en Media y la categoría es opcional.",
            ),
            LangostinAction.Answer(
                label = "¿Quién lo va a ver?",
                answer = "Queda creado a tu nombre y lo ve quien tenga el permiso de tickets con su alcance.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pasa al guardar?",
                answer = "Se crea el ticket y te lleva a su detalle para seguir el estado y comentar.",
            ),
            LangostinAction.GoTo(
                label = "Ver la lista de tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val ticketsKanban = LangostinScreen(
        id = "tickets.kanban",
        title = "Tablero de tareas",
        explain = "Las tareas en columnas por estado: Pendiente, En progreso, En revisión y Completada.",
        match = { it is Screen.TicketsKanban },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo muevo una tarea?",
                answer = "Toca la tarjeta y elige el estado nuevo en «Mover a». El cambio se guarda al instante.",
            ),
            LangostinAction.Answer(
                label = "¿Qué es «En revisión»?",
                answer = "La persona terminó la tarea y falta que alguien la valide para completarla.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo creo una tarea aquí?",
                answer = "Con el botón +, si puedes asignar tareas: pide ticket, persona, título y fecha límite.",
            ),
            LangostinAction.GoTo(
                label = "Ver mis tareas",
                answer = "Te llevo a la lista de tus tareas asignadas.",
                to = Screen.MyTasks,
                permission = P.TASKS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver la lista de tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val deviceDetail = LangostinScreen(
        id = "devices.detail",
        title = "Detalle del dispositivo",
        explain = "La ficha del dispositivo: datos, estado, cartas responsivas activas e historial.",
        match = { it is Screen.DeviceDetail },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo registro un movimiento?",
                answer = "Toca «Movimiento» para registrar entrada, salida, traslado, baja o asignación.",
            ),
            LangostinAction.Answer(
                label = "¿Por qué no puedo editarlo?",
                answer = "Si está asignado, el formulario bloquea la edición; registra primero su devolución.",
            ),
            LangostinAction.Answer(
                label = "¿Qué es una carta activa?",
                answer = "Un préstamo firmado que todavía no se devuelve.",
            ),
            LangostinAction.GoTo(
                label = "Ver el kardex",
                answer = "Te llevo al historial de movimientos del inventario.",
                to = Screen.InventoryMovements,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Volver a dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val deviceForm = LangostinScreen(
        id = "devices.form",
        title = "Dispositivo",
        explain = "Alta o edición de un dispositivo. El tipo define qué campos se piden.",
        match = { it is Screen.DeviceForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué datos son obligatorios?",
                answer = "Tipo y descripción. El resto depende de lo que el tipo marque como obligatorio.",
            ),
            LangostinAction.Answer(
                label = "¿Por qué no puedo guardar?",
                answer = "El dispositivo está bloqueado si ya está asignado: registra su devolución antes de editarlo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué son los campos por tipo?",
                answer = "Cada tipo decide qué datos lleva: serie, MAC, IP, nombre de equipo, sistema, RAM o almacenamiento.",
            ),
            LangostinAction.GoTo(
                label = "Volver a dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val deviceTypeForm = LangostinScreen(
        id = "devices.typeForm",
        title = "Tipo de dispositivo",
        explain = "Define nombre, prefijo, campos y estatus de un tipo de dispositivo.",
        match = { it is Screen.DeviceTypeForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Puedo cambiar el código o el prefijo?",
                answer = "No se cambian después de crear el tipo. Solo puedes actualizar nombre, estatus y campos.",
            ),
            LangostinAction.Answer(
                label = "¿Qué hace «Obligatorio»?",
                answer = "El campo se exige al dar de alta un dispositivo de ese tipo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pasa si desactivo el tipo?",
                answer = "Deja de ofrecerse para altas nuevas, pero los dispositivos existentes lo conservan.",
            ),
            LangostinAction.GoTo(
                label = "Volver a tipos",
                answer = "Te llevo a la lista de tipos de dispositivo.",
                to = Screen.DeviceTypesList,
                permission = P.CATALOGS_MANAGE,
            ),
        ),
    )

    private val employeeForm = LangostinScreen(
        id = "employees.form",
        title = "Cuenta del personal",
        explain = "Alta o edición de la cuenta del personal: datos de acceso, perfil, rol y departamento.",
        match = { it is Screen.UserForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo cambio la contraseña?",
                answer = "Escribe la nueva contraseña. Si la dejas en blanco, no cambia.",
            ),
            LangostinAction.Answer(
                label = "¿Qué son los tres pasos?",
                answer = "Acceso, perfil y organización. Con Continuar avanzas y guardas al final.",
            ),
            LangostinAction.Answer(
                label = "¿Quién puede editar cuentas?",
                answer = "Requiere permiso de creación o edición de usuarios; la app solo esconde el botón.",
            ),
            LangostinAction.GoTo(
                label = "Ver el personal",
                answer = "Te llevo al directorio de personal.",
                to = Screen.EmployeesList,
                permission = P.HR_RECORDS,
            ),
        ),
    )

    private val employeeProfile = LangostinScreen(
        id = "employees.profile",
        title = "Perfil de la persona",
        explain = "La ficha de la persona: contacto, puesto, datos personales, domicilio y documentos.",
        match = { it is Screen.EmployeeProfile },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo edito sus datos?",
                answer = "Toca «Editar personal» para actualizar sus datos, su rol y su departamento.",
            ),
            LangostinAction.Answer(
                label = "¿Qué significa el porcentaje de documentos?",
                answer = "Es la parte de tipos obligatorios ya subidos; los pendientes se marcan en la lista.",
            ),
            LangostinAction.Answer(
                label = "¿Dónde veo su cuenta y acceso?",
                answer = "Con «Usuarios» abres su cuenta: usuario, contraseña, rol, departamento y subárea.",
            ),
            LangostinAction.GoTo(
                label = "Volver al personal",
                answer = "Te llevo al directorio de personal.",
                to = Screen.EmployeesList,
                permission = P.HR_RECORDS,
            ),
        ),
    )

    private val departmentDetail = LangostinScreen(
        id = "departments.detail",
        title = "Detalle del departamento",
        explain = "El detalle del departamento: personas, áreas, ubicaciones, tickets y cartas.",
        match = { it is Screen.DepartmentDetail },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo agrego un área?",
                answer = "Escribe el nombre en «Nueva subárea» y toca Agregar.",
            ),
            LangostinAction.Answer(
                label = "¿Qué muestran las ubicaciones?",
                answer = "Los lugares del departamento con sus dispositivos y cartas responsivas.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo eliminar el departamento?",
                answer = "No si tiene personas activas. La primera eliminación lo desactiva; la segunda lo borra.",
            ),
            LangostinAction.GoTo(
                label = "Volver a departamentos",
                answer = "Te llevo a la lista de departamentos.",
                to = Screen.DepartmentsList,
                permission = P.DEPARTMENTS_MANAGE,
            ),
        ),
    )

    private val locationDetail = LangostinScreen(
        id = "inventory.locationDetail",
        title = "Detalle de la ubicación",
        explain = "El detalle de una ubicación: sublugares y los dispositivos y cartas que tiene.",
        match = { it is Screen.LocationDetail },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo agrego un sublugar?",
                answer = "Escribe el nombre en «Nuevo sublugar» y toca Agregar.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo eliminar un sublugar?",
                answer = "Sí, con la X de su fila. Pide confirmación antes de quitarlo.",
            ),
            LangostinAction.Answer(
                label = "¿Dónde abro un dispositivo?",
                answer = "Aquí solo se listan; para abrir su ficha entra a Dispositivos.",
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Volver a ubicaciones",
                answer = "Te llevo a la lista de ubicaciones.",
                to = Screen.LocationsList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val locationForm = LangostinScreen(
        id = "inventory.locationForm",
        title = "Ubicación",
        explain = "Alta o edición de una ubicación: nombre y descripción.",
        match = { it is Screen.LocationForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo creo una ubicación?",
                answer = "Escribe el nombre y toca Crear ubicación. Si estás editando, usa Guardar cambios.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pasa con sus sublugares?",
                answer = "Se administran desde el detalle de la ubicación, no en este formulario.",
            ),
            LangostinAction.GoTo(
                label = "Volver a ubicaciones",
                answer = "Te llevo a la lista de ubicaciones.",
                to = Screen.LocationsList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val movementNew = LangostinScreen(
        id = "inventory.movementNew",
        title = "Registrar movimiento",
        explain = "Registra una entrada, salida, traslado, baja o asignación de un dispositivo.",
        match = { it is Screen.NewInventoryMovement },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué tipo elijo?",
                answer = "Entrada suma al inventario, salida lo retira, traslado cambia de ubicación, baja lo saca para siempre y asignado lo entrega a una persona.",
            ),
            LangostinAction.Answer(
                label = "¿Se puede deshacer?",
                answer = "Desde la app no se deshace. Revisa bien antes de registrar; si te equivocas, avisa a Sistemas.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pide la asignación?",
                answer = "A quién se entrega, fecha de retorno y condición. Si regresa mal, eliges darlo de baja o solo registrarlo.",
            ),
            LangostinAction.GoTo(
                label = "Ver el kardex",
                answer = "Te llevo al historial de movimientos del inventario.",
                to = Screen.InventoryMovements,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val materialOutputForm = LangostinScreen(
        id = "materialOutputs.form",
        title = "Salida de material",
        explain = "Registra una salida de material: descripción, cantidad, departamento y responsable.",
        match = { it is Screen.MaterialOutputForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es una salida de material?",
                answer = "Material que sale del almacén y no se devuelve. No es un préstamo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué campos son obligatorios?",
                answer = "Descripción y cantidad. Los demás datos ayudan a ubicar el gasto.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo la edito después?",
                answer = "Toca el registro en la lista para abrir este formulario y guardar los cambios.",
            ),
            LangostinAction.GoTo(
                label = "Ver las salidas",
                answer = "Te llevo a la lista de salidas de material.",
                to = Screen.MaterialOutputsList,
                permission = P.MATERIAL_OUTPUTS_REGISTER,
            ),
        ),
    )

    private val custodyLetterDetail = LangostinScreen(
        id = "custodyLetters.detail",
        title = "Carta responsiva",
        explain = "La carta responsiva: recurso, responsable, firmantes y devolución.",
        match = { it is Screen.CustodyLetterDetail },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo marco la devolución?",
                answer = "Toca «Marcar devuelto» y captura la fecha, las condiciones y quién la recibe.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo cancelar una devolución?",
                answer = "Sí, con «Cancelar devolución». El dispositivo vuelve a quedar asignado.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo elimino la carta?",
                answer = "Con «Eliminar», si tienes permiso de cancelar préstamos. Pide confirmación.",
            ),
            LangostinAction.GoTo(
                label = "Volver a cartas",
                answer = "Te llevo a la lista de cartas responsivas.",
                to = Screen.CustodyLettersList,
                permission = P.LOANS_VIEW,
            ),
        ),
    )

    private val custodyLetterForm = LangostinScreen(
        id = "custodyLetters.form",
        title = "Carta responsiva",
        explain = "Alta o edición de una carta: dispositivo, persona y firmantes.",
        match = { it is Screen.CustodyLetterForm },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo elijo el dispositivo?",
                answer = "Primero el tipo y después una unidad disponible. Si editas, puedes cambiarlo con «Cambiar dispositivo».",
            ),
            LangostinAction.Answer(
                label = "¿Quién firma?",
                answer = "La persona que recibe, el jefe de área y quien entrega. Búscalos por nombre.",
            ),
            LangostinAction.Answer(
                label = "¿Por qué no aparece un equipo?",
                answer = "Solo se ofrecen unidades disponibles; una ya asignada no se puede elegir.",
            ),
            LangostinAction.GoTo(
                label = "Ver las cartas",
                answer = "Te llevo a la lista de cartas responsivas.",
                to = Screen.CustodyLettersList,
                permission = P.LOANS_VIEW,
            ),
        ),
    )

    private val custodyLetterGenerate = LangostinScreen(
        id = "custodyLetters.generate",
        title = "Generar cartas",
        explain = "Genera una carta responsiva nueva a partir de un tipo de dispositivo.",
        match = { it is Screen.GenerateCustodyLetter },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué hace exactamente?",
                answer = "Crea una carta con el tipo que elijas. Al terminar puedes abrir la carta generada.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo la termino de llenar?",
                answer = "Abre la carta generada para asignar responsable y firmantes.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo generar varias?",
                answer = "Sí. Cancela al terminar una y repite el flujo para otro tipo.",
            ),
            LangostinAction.GoTo(
                label = "Ver las cartas",
                answer = "Te llevo a la lista de cartas responsivas.",
                to = Screen.CustodyLettersList,
                permission = P.LOANS_VIEW,
            ),
        ),
    )

    // ── Listas, índices y pantallas de operación ────────────────────────────

    private val home = LangostinScreen(
        id = "home",
        title = "Inicio",
        explain = "El inicio resume la operación en pestañas: Operación, Personal, Reloj y accesos, Sistema y Mi espacio.",
        match = { it is Screen.Home },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué significa cada tarjeta?",
                answer = "Cada tarjeta muestra un dato real; el color avisa: verde bien, ámbar atención, rojo problema. Al tocarla se abre su pantalla.",
            ),
            LangostinAction.Answer(
                label = "¿Por qué veo unas pestañas y no otras?",
                answer = "Dependen de tus permisos. Si te falta alguna, pídela a Sistemas o a RH.",
            ),
            LangostinAction.Answer(
                label = "¿Cada cuánto se actualiza?",
                answer = "La píldora «En vivo» indica que el inicio se refresca solo cada minuto.",
            ),
            LangostinAction.GoTo(
                label = "Ver tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver personal",
                answer = "Te llevo al directorio de personal.",
                to = Screen.EmployeesList,
                permission = P.HR_RECORDS,
            ),
            LangostinAction.GoTo(
                label = "Ver inventario",
                answer = "Te llevo al resumen del inventario.",
                to = Screen.InventoryIndex,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val credential = LangostinScreen(
        id = "credential",
        title = "Mi credencial",
        explain = "Tu credencial digital: muéstrale el código QR al guardia al entrar y al salir.",
        match = { it is Screen.MyCredential },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo la uso?",
                answer = "Abre esta pantalla y muestra el código. El guardia lo escanea en Portería.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pasa si falla la carga?",
                answer = "El aviso de abajo no invalida el código: tu credencial sigue sirviendo.",
            ),
            LangostinAction.Answer(
                label = "¿De dónde salen mis datos?",
                answer = "De tu cuenta: nombre, puesto, departamento y número de empleado.",
            ),
            LangostinAction.GoTo(
                label = "Ir al inicio",
                answer = "Te llevo al inicio.",
                to = Screen.Home,
            ),
        ),
    )

    private val ticketsList = LangostinScreen(
        id = "tickets.list",
        title = "Tickets",
        explain = "Los tickets de soporte, con búsqueda y filtros por estado.",
        match = { it is Screen.TicketsList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo filtro?",
                answer = "Toca una píldora de estado (Todos, Abiertos, Seguimiento, Cerrados) y usa el buscador por título o descripción.",
            ),
            LangostinAction.Answer(
                label = "¿Qué significa cada estado?",
                answer = "Abierto es nuevo, En seguimiento ya se está trabajando y Cerrado ya terminó.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo creo un ticket?",
                answer = "Con el botón +, si tienes permiso para crear tickets.",
            ),
            LangostinAction.GoTo(
                label = "Ver el tablero",
                answer = "Te llevo al tablero de tareas.",
                to = Screen.TicketsKanban(),
                permission = P.TICKETS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Crear un ticket",
                answer = "Te llevo al alta de ticket.",
                to = Screen.NewTicket,
                permission = P.TICKETS_CREATE,
            ),
        ),
    )

    private val myTasks = LangostinScreen(
        id = "tickets.myTasks",
        title = "Mis tareas",
        explain = "Las tareas de tickets que tienes asignadas, con su fecha límite y si están vencidas.",
        match = { it is Screen.MyTasks },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es una tarea vencida?",
                answer = "La fecha límite ya pasó y la tarea no está completada. Arriba verás cuántas son.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo abro el ticket?",
                answer = "Toca la tarea y te lleva al detalle del ticket.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo veo el tablero?",
                answer = "Usa «Ver tablero kanban» para ver las tareas por estado.",
            ),
            LangostinAction.GoTo(
                label = "Ir al tablero",
                answer = "Te llevo al tablero de tareas.",
                to = Screen.TicketsKanban(),
                permission = P.TICKETS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val adminTasks = LangostinScreen(
        id = "tickets.adminTasks",
        title = "Administración de tareas",
        explain = "Todas las tareas de todos los tickets, con su fecha límite y cuántas están vencidas.",
        match = { it is Screen.AdminTasks },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué permiso necesito?",
                answer = "El permiso de completar tareas. Sin él la pantalla no abre.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo sé cuáles están vencidas?",
                answer = "Arriba aparece el contador rojo de vencidas y cada tarea muestra su fecha límite.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo abro el ticket de una tarea?",
                answer = "Toca la tarea y te lleva al detalle del ticket.",
            ),
            LangostinAction.GoTo(
                label = "Ir al tablero",
                answer = "Te llevo al tablero de tareas.",
                to = Screen.TicketsKanban(),
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val devicesList = LangostinScreen(
        id = "devices.list",
        title = "Dispositivos",
        explain = "El catálogo de dispositivos: activo fijo, descripción, tipo, estado y ubicación.",
        match = { it is Screen.DevicesList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo busco un equipo?",
                answer = "Escribe activo, marca o modelo en el buscador y combina con las píldoras de estado.",
            ),
            LangostinAction.Answer(
                label = "¿Qué significa cada estado?",
                answer = "Disponible se puede prestar, asignado está en una carta, dañado o en mantenimiento no se presta y baja ya salió del inventario.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo abro la ficha de un equipo?",
                answer = "Toca su tarjeta para ver datos, cartas activas e historial.",
            ),
            LangostinAction.GoTo(
                label = "Ver los tipos de equipo",
                answer = "Te llevo al catálogo de tipos de dispositivo.",
                to = Screen.DeviceTypesList,
                permission = P.CATALOGS_MANAGE,
            ),
            LangostinAction.GoTo(
                label = "Ver el kardex",
                answer = "Te llevo al historial de movimientos del inventario.",
                to = Screen.InventoryMovements,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val deviceTypesList = LangostinScreen(
        id = "devices.types",
        title = "Tipos de dispositivo",
        explain = "Cada tipo define el prefijo del activo fijo y qué datos lleva cada unidad.",
        match = { it is Screen.DeviceTypesList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Para qué sirve el prefijo?",
                answer = "Inicia el activo fijo: con LPT el siguiente equipo es LPT-0001 y el consecutivo sigue por tipo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué es «Siguiente»?",
                answer = "El folio que tendrá la próxima unidad dada de alta de ese tipo.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo desactivar un tipo?",
                answer = "Sí, desde su edición lo marcas como inactivo y deja de ofrecerse en altas nuevas.",
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val employeesList = LangostinScreen(
        id = "employees.list",
        title = "Personal",
        explain = "El directorio de personas: quién está activo, su puesto y su departamento.",
        match = { it is Screen.EmployeesList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo abro el perfil de alguien?",
                answer = "Toca su tarjeta o el botón «Perfil» para ver su expediente y sus documentos.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo filtro la lista?",
                answer = "Usa el buscador por nombre y los filtros de departamento, estatus y rol.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo cambio su rol o cuenta?",
                answer = "Con «Editar» abres su cuenta: usuario, contraseña, rol, departamento y subárea.",
            ),
            LangostinAction.GoTo(
                label = "Ver departamentos",
                answer = "Te llevo a la lista de departamentos.",
                to = Screen.DepartmentsList,
                permission = P.DEPARTMENTS_MANAGE,
            ),
        ),
    )

    private val departmentsList = LangostinScreen(
        id = "departments.list",
        title = "Departamentos",
        explain = "La estructura de la organización: departamentos, áreas y cuántas personas tiene cada uno.",
        match = { it is Screen.DepartmentsList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo creo un departamento?",
                answer = "Con «Nuevo departamento». Desde su fila también puedes editarlo o abrirlo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué es un área?",
                answer = "Una división dentro del departamento. Se administra en el detalle del departamento.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo se elimina un departamento?",
                answer = "No se puede si tiene personas activas. La primera eliminación lo desactiva y la segunda lo borra.",
            ),
            LangostinAction.GoTo(
                label = "Ver el personal",
                answer = "Te llevo al directorio de personal.",
                to = Screen.EmployeesList,
                permission = P.HR_RECORDS,
            ),
            LangostinAction.GoTo(
                label = "Ver ubicaciones",
                answer = "Te llevo a la lista de ubicaciones.",
                to = Screen.LocationsList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val inventoryIndex = LangostinScreen(
        id = "inventory.index",
        title = "Inventario",
        explain = "El resumen del inventario: disponibilidad, ubicaciones y accesos a movimientos.",
        match = { it is Screen.InventoryIndex },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué muestra esta pantalla?",
                answer = "El total de dispositivos y cuántos están en una ubicación o sin ella.",
            ),
            LangostinAction.Answer(
                label = "¿Dónde veo las ubicaciones?",
                answer = "En «Ubicaciones» está la lista completa; toca una para ver su detalle.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo registro un movimiento?",
                answer = "Con «Nuevo movimiento», si tienes permiso para editar dispositivos.",
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver el kardex",
                answer = "Te llevo al historial de movimientos del inventario.",
                to = Screen.InventoryMovements,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver ubicaciones",
                answer = "Te llevo a la lista de ubicaciones.",
                to = Screen.LocationsList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val locationsList = LangostinScreen(
        id = "inventory.locations",
        title = "Ubicaciones",
        explain = "Las ubicaciones donde están los equipos, con sus sublugares.",
        match = { it is Screen.LocationsList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo creo una ubicación?",
                answer = "Con «Nueva ubicación». El nombre es el único dato obligatorio.",
            ),
            LangostinAction.Answer(
                label = "¿Qué son los sublugares?",
                answer = "Divisiones dentro de la ubicación. Se agregan desde el detalle de la ubicación.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo elimino una ubicación?",
                answer = "Con el botón de eliminar de su fila: pide confirmación y la primera vez solo la desactiva.",
            ),
            LangostinAction.GoTo(
                label = "Ver el inventario",
                answer = "Te llevo al resumen del inventario.",
                to = Screen.InventoryIndex,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val inventoryMovements = LangostinScreen(
        id = "inventory.movements",
        title = "Kardex",
        explain = "El historial de movimientos: entradas, salidas, traslados y bajas, con quién y cuándo.",
        match = { it is Screen.InventoryMovements },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo filtro?",
                answer = "Elige ubicación y un rango de fechas, y toca «Aplicar filtros».",
            ),
            LangostinAction.Answer(
                label = "¿Qué es una entrada?",
                answer = "Suma una pieza al inventario. La salida la retira y la baja la saca para siempre.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo registro uno nuevo?",
                answer = "Con «Nuevo movimiento». También puedes abrirlo desde el detalle de un dispositivo.",
            ),
            LangostinAction.GoTo(
                label = "Registrar movimiento",
                answer = "Te llevo al alta de movimiento.",
                to = Screen.NewInventoryMovement(),
                permission = P.DEVICES_EDIT,
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val materialOutputsList = LangostinScreen(
        id = "materialOutputs.list",
        title = "Salidas de material",
        explain = "Las salidas de material registradas, con búsqueda por descripción, departamento o usuario.",
        match = { it is Screen.MaterialOutputsList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es una salida?",
                answer = "Material que sale del almacén y no se devuelve. Queda como registro del gasto.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo registro una?",
                answer = "Con «Nueva salida»: describe el material y la cantidad que sale.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo la elimino?",
                answer = "Con el botón de eliminar de su fila. Pide confirmación.",
            ),
            LangostinAction.GoTo(
                label = "Registrar una salida",
                answer = "Te llevo al alta de salida de material.",
                to = Screen.MaterialOutputForm(),
                permission = P.MATERIAL_OUTPUTS_REGISTER,
            ),
        ),
    )

    private val custodyLettersList = LangostinScreen(
        id = "custodyLetters.list",
        title = "Cartas responsivas",
        explain = "Las cartas responsivas: qué equipo tiene cada persona y si ya se devolvió.",
        match = { it is Screen.CustodyLettersList },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo creo una carta?",
                answer = "Con «Nueva carta» eliges tipo y dispositivo. «Generar carta por tipo» crea una desde el tipo.",
            ),
            LangostinAction.Answer(
                label = "¿Qué significa «Devuelta»?",
                answer = "El equipo ya regresó. La carta queda en el historial marcada como devuelta.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo elimino una carta?",
                answer = "Con el botón de eliminar de su fila, si tienes permiso. Pide confirmación.",
            ),
            LangostinAction.GoTo(
                label = "Generar carta por tipo",
                answer = "Te llevo al alta de cartas por tipo.",
                to = Screen.GenerateCustodyLetter,
                permission = P.LOANS_CREATE,
            ),
            LangostinAction.GoTo(
                label = "Nueva carta",
                answer = "Te llevo al alta de carta responsiva.",
                to = Screen.CustodyLetterForm(),
                permission = P.LOANS_CREATE,
            ),
        ),
    )

    private val accessScan = LangostinScreen(
        id = "access.scan",
        title = "Portería",
        explain = "Escanea el código QR de la credencial para registrar una entrada o una salida.",
        match = { it is Screen.AccessScan },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo registro un acceso?",
                answer = "Elige la portería, apunta el QR al recuadro y confirma el tipo: entrada o salida.",
            ),
            LangostinAction.Answer(
                label = "¿Qué pasa con la ubicación?",
                answer = "Si no das permiso de ubicación, el registro se guarda solo con la portería.",
            ),
            LangostinAction.Answer(
                label = "¿Qué hago si la cámara no abre?",
                answer = "Revisa el permiso de cámara en los ajustes del teléfono y vuelve a intentar.",
            ),
            LangostinAction.GoTo(
                label = "Ver registros de acceso",
                answer = "Te llevo a los registros de acceso.",
                to = Screen.AccessLog,
                permission = P.ACCESS_SCAN,
            ),
        ),
    )

    private val accessLog = LangostinScreen(
        id = "access.log",
        title = "Registros de acceso",
        explain = "Los registros de entrada y salida de portería, con fecha, tipo y si están anulados.",
        match = { it is Screen.AccessLog },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es un registro anulado?",
                answer = "Un acceso que se corrigió después. Sigue visible para auditoría pero no cuenta.",
            ),
            LangostinAction.Answer(
                label = "¿Qué registros veo?",
                answer = "Con permiso de bitácora ves todos. Sin él, solo tus escaneos de hoy.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo paso de página?",
                answer = "Usa Anterior y Siguiente abajo. La bitácora completa muestra 20 registros por página.",
            ),
            LangostinAction.GoTo(
                label = "Escanear credencial",
                answer = "Te llevo a la portería para escanear.",
                to = Screen.AccessScan,
                permission = P.ACCESS_SCAN,
            ),
        ),
    )

    private val reports = LangostinScreen(
        id = "reports",
        title = "Reportes",
        explain = "Reportes del inventario en dos pestañas: equipos asignados y dispositivos.",
        match = { it is Screen.Reports },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué muestra «Asignados»?",
                answer = "Los equipos en manos de alguien, con su responsable, los días asignados y los que pasan de 30 días.",
            ),
            LangostinAction.Answer(
                label = "¿Qué muestra «Dispositivos»?",
                answer = "El total por estado: disponibles, asignados y de baja.",
            ),
            LangostinAction.Answer(
                label = "¿De dónde salen los datos?",
                answer = "Del inventario vigente. Si cambia un movimiento, el reporte lo refleja al recargar.",
            ),
            LangostinAction.GoTo(
                label = "Ver cartas responsivas",
                answer = "Te llevo a la lista de cartas responsivas.",
                to = Screen.CustodyLettersList,
                permission = P.LOANS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver dispositivos",
                answer = "Te llevo a la lista de dispositivos.",
                to = Screen.DevicesList,
                permission = P.DEVICES_VIEW,
            ),
        ),
    )

    private val notifications = LangostinScreen(
        id = "notifications",
        title = "Notificaciones",
        explain = "Tus avisos: comentarios, asignaciones y cambios de tickets.",
        match = { it is Screen.Notifications },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo abro un aviso?",
                answer = "Toca la notificación: se marca como leída y te lleva al ticket si lo tiene.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo marco todo como leído?",
                answer = "Usa «Marcar todo leído» arriba, cuando hay avisos pendientes.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo borrar avisos?",
                answer = "Sí, con el ícono de eliminar de cada tarjeta. Se quita solo de tu lista.",
            ),
            LangostinAction.GoTo(
                label = "Ver tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
        ),
    )

    private val auditLogs = LangostinScreen(
        id = "audit",
        title = "Auditoría",
        explain = "La bitácora de auditoría: qué cambió, quién y cuándo, con filtros por tipo y fecha.",
        match = { it is Screen.AuditLogs },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué registra?",
                answer = "Entradas, salidas, traslados, bajas, préstamos, devoluciones y cartas, con usuario y notas.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo filtro?",
                answer = "Elige el tipo de acción y un rango de fechas, y toca «Aplicar filtros».",
            ),
            LangostinAction.Answer(
                label = "¿Puedo editar un registro?",
                answer = "No. La bitácora es solo lectura: sirve de rastro de lo que pasó.",
            ),
            LangostinAction.GoTo(
                label = "Ver el kardex",
                answer = "Te llevo al historial de movimientos del inventario.",
                to = Screen.InventoryMovements,
                permission = P.DEVICES_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ver registros de acceso",
                answer = "Te llevo a la bitácora de portería.",
                to = Screen.AccessLog,
                permission = P.ACCESS_LOG,
            ),
        ),
    )

    private val roles = LangostinScreen(
        id = "roles",
        title = "Roles y permisos",
        explain = "Roles y permisos: la matriz de alcance por rol y el catálogo de permisos.",
        match = { it is Screen.RolesPermissions },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Qué es la Matriz?",
                answer = "Define, por rol y permiso, el alcance: propio, área o todo, además de Ninguno.",
            ),
            LangostinAction.Answer(
                label = "¿Cómo guardo los cambios?",
                answer = "Revisa cuántos cambios pendientes hay y toca Guardar. Descarta los anula.",
            ),
            LangostinAction.Answer(
                label = "¿Puedo eliminar un permiso?",
                answer = "No. Los permisos no se eliminan, solo se desactivan, y un permiso inactivo no se concede.",
            ),
            LangostinAction.GoTo(
                label = "Ver el personal",
                answer = "Te llevo al directorio de personal para revisar su rol.",
                to = Screen.EmployeesList,
                permission = P.HR_RECORDS,
            ),
        ),
    )

    // ── Default: siempre casa y va al final ────────────────────────────────

    private val default = LangostinScreen(
        id = "default",
        title = "Puerto Nuevo",
        explain = "Estoy para explicarte cómo usar el sistema. Elige una opción o pregúntame por una pantalla.",
        match = { true },
        actions = listOf(
            LangostinAction.Answer(
                label = "¿Cómo creo un ticket?",
                answer = "Entra a Tickets y usa el botón +, si tienes permiso. El ticket queda a tu nombre.",
            ),
            LangostinAction.Answer(
                label = "¿Dónde está cada cosa?",
                answer = "Usa el menú lateral: Inicio, Tickets, Inventario, Recursos Humanos y Configuración.",
            ),
            LangostinAction.Answer(
                label = "Si algo no funciona",
                answer = "Avisa a Sistemas con el detalle de lo que intentabas hacer. Si es urgente, levanta un ticket.",
            ),
            LangostinAction.GoTo(
                label = "Ir a Tickets",
                answer = "Te llevo a la lista de tickets.",
                to = Screen.TicketsList,
                permission = P.TICKETS_VIEW,
            ),
            LangostinAction.GoTo(
                label = "Ir al inicio",
                answer = "Te llevo al inicio.",
                to = Screen.Home,
            ),
        ),
    )

    /** Entradas en orden; la última es el `default` que siempre casa. */
    val screens: List<LangostinScreen> = listOf(
        // Detalles y formularios
        ticketDetail,
        ticketEdit,
        ticketNew,
        ticketsKanban,
        deviceDetail,
        deviceForm,
        deviceTypeForm,
        employeeForm,
        employeeProfile,
        departmentDetail,
        locationDetail,
        locationForm,
        movementNew,
        materialOutputForm,
        custodyLetterDetail,
        custodyLetterForm,
        custodyLetterGenerate,
        // Listas, índices y operación
        home,
        credential,
        ticketsList,
        myTasks,
        adminTasks,
        devicesList,
        deviceTypesList,
        employeesList,
        departmentsList,
        inventoryIndex,
        locationsList,
        inventoryMovements,
        materialOutputsList,
        custodyLettersList,
        accessScan,
        accessLog,
        reports,
        notifications,
        auditLogs,
        roles,
        // Default (siempre casa)
        default,
    )

    /** La entrada que corresponde a una pantalla (siempre hay una: `default`). */
    fun forScreen(screen: Screen): LangostinScreen =
        screens.firstOrNull { it.match(screen) } ?: default
}
