package common
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import java.io.File

object Configurations{
  def getConfig(name: String): Config = {
    val configFile = getClass.getClassLoader.getResource(name).getFile
    ConfigFactory.parseFile(new File(configFile))
  }
}