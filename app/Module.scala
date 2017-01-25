import com.google.inject.AbstractModule
import java.time.Clock
import services.SpamCommentRemover

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[SpamCommentRemover]).asEagerSingleton()
  }
}
