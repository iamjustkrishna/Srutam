package space.iamjustkrishna.srutam.cloud

data class ApiKeyItem(
    val id: String,
    val name: String,
    val keyPrefix: String,
    val createdAt: String,
    val lastUsedAt: String?
)
