package space.iamjustkrishna.srutam

import android.app.Application
import space.iamjustkrishna.srutam.data.AppDatabase
import space.iamjustkrishna.srutam.utils.AudioFileReader

class SrutamApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AudioFileReader.init(this)
    }

    companion object {
        @Volatile
        private var instance: SrutamApplication? = null

        fun getInstance(): SrutamApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
