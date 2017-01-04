package helpers

object Sanitizer {
  def forUrl(url: String): String = 
    if (url.trim.startsWith("//")) "/"
    else if (url.indexOf("://") != -1) "/"
    else if (url.trim.isEmpty) "/"
    else url
}
