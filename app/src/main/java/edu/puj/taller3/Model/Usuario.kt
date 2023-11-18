package edu.puj.taller3.Model

class Usuario {
    var key: String? = null
    var name: String? = null
    var apellido: String? = null
    var id = 0
    var latitude = 0.0
    var longitude = 0.0
    var disponible = false
    lateinit var photo: ByteArray

    constructor()
    constructor(
        key: String?,
        name: String?,
        apellido: String?,
        id: Int,
        latitude: Double,
        longitude: Double,
        disponible: Boolean
    ) {
        this.key = key
        this.name = name
        this.apellido = apellido
        this.id = id
        this.latitude = latitude
        this.longitude = longitude
        this.disponible = disponible
    }
}