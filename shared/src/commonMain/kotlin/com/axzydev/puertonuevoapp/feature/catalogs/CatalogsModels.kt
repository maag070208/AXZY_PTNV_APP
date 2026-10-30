package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Pestañas de Catálogos, en el mismo orden que la web (organización, personal,
 * tickets, sistema, inventario). La app agrupa Departamentos y Subáreas en una
 * sola pestaña y deja Tipos de dispositivo como acceso a su pantalla propia.
 */
enum class CatalogsTab(val label: String, val icon: ImageVector) {
    Departments("Departamentos y subáreas", Icons.Filled.Business),
    DocumentTypes("Tipos de documento", Icons.Filled.Description),
    Genders("Géneros", Icons.Filled.Person),
    BloodTypes("Tipos de sangre", Icons.Filled.FavoriteBorder),
    TicketCategories("Categorías de ticket", Icons.Filled.Sell),
    Notifications("Notificaciones", Icons.Filled.Email),
    DeviceTypes("Tipos de dispositivo", Icons.Filled.Devices),
}

/** Un renglón de un catálogo simple: nombre y estatus. */
data class SimpleCatalogEntry(
    val id: String,
    val name: String,
    val active: Boolean,
)

/**
 * Adaptador de un catálogo simple. Los cuatro catálogos de la pestaña
 * (tipos de documento, géneros, tipos de sangre y categorías de ticket)
 * comparten forma y CRUD; solo cambian sus llamadas al API.
 *
 * Las altas y ediciones de todos ellos exigen `catalogs.manage` en el API; la
 * lectura de los de RH exige además `hr.records` ([viewPermission]).
 */
data class SimpleCatalogSource(
    /** Nombre del catálogo para encabezados y diálogos. */
    val label: String,
    /** Singular para "Nuevo …" / "Editar …". */
    val singular: String,
    val emptyMessage: String,
    /** Permiso necesario para consultar; `null` = cualquier sesión. */
    val viewPermission: String? = null,
    val deniedMessage: String = "No tienes permiso para consultar este catálogo.",
    val list: suspend (includeInactive: Boolean) -> List<SimpleCatalogEntry>,
    val create: suspend (name: String) -> Unit,
    val update: suspend (id: String, name: String?, active: Boolean?) -> Unit,
    val remove: suspend (id: String) -> Unit,
)
