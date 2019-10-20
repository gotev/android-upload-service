package net.gotev.uploadservice.ftp

/**
 * Utility class to work with UNIX permissions.
 *
 * The default permissions set are 644 (Owner can read and write. Group and World can only read)
 */
class UnixPermissions @JvmOverloads constructor(value: String = "644") {

    data class RolePermissions(
        var read: Boolean = false,
        var write: Boolean = false,
        var execute: Boolean = false
    ) {
        companion object {
            fun fromChar(char: Char): RolePermissions {
                var mutableValue = char.toString().toInt()

                val read = mutableValue >= 4
                if (read) mutableValue -= 4

                val write = mutableValue >= 2
                if (write) mutableValue -= 2

                val execute = mutableValue == 1
                if (execute) mutableValue -= 1

                return RolePermissions(read, write, execute)
            }
        }

        override fun toString(): String {
            var value = 0

            if (read) value += 4
            if (write) value += 2
            if (execute) value += 1

            return value.toString()
        }
    }

    val owner: RolePermissions
    val group: RolePermissions
    val world: RolePermissions

    init {
        require(value.isNotBlank() && value.length == 3) {
            "UNIX permissions value length must be 3!"
        }

        val permissions = value.map {
            require(it.isDigit()) { "UNIX permissions value must be numeric" }
            RolePermissions.fromChar(it)
        }

        owner = permissions[0]
        group = permissions[1]
        world = permissions[2]
    }

    override fun toString() = "$owner$group$world"
}
