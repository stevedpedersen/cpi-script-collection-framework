package src.main.resources.script

abstract class JarLocation {
	URI uri
	String schemePart
	JarLocation(URI uri){
		this.uri = uri
		schemePart = uri.schemeSpecificPart
	}

	boolean isUnknown() {
		return false;
	}

	boolean isLocal() {
		if(getURL().host) {
			return false
		}
		else {
			return true
		}
	}

	abstract String getFilename()
	abstract URL getURL()
}

class UnknownLocation extends JarLocation{
	UnknownLocation (URI uri){
		super(uri)
	}

	@Override
	boolean isUnknown() {
		return true;
	}

	@Override
	public String getFilename() {
		return schemePart;
	}

	@Override
	public URL getURL() {
		return null;
	}
}

class MavenJar extends JarLocation {
	String repositoryURL = "https://repo1.maven.org/maven2/"

	MavenJar(URI uri){
		super(uri)
	}

	String setRepositoryURL(String url) {
		if(url) {
			if(url.endsWith('/'))
				repositoryURL = url
			else
				repositoryURL = url + '/'
		}
	}

	String getFilename() {
		URL url = getURL()
		int fileBegin = url.path.lastIndexOf('/')
		return url.path.substring(fileBegin+1)
	}

	URL getURL(){
		return buildURL()
	}

	URL buildURL(){
		return new URL(repositoryURL + mavenToPath(schemePart))
	}

	private String mavenToPath(String mvnPath) {
		String[] parts = mvnPath.split('/')
		String group = parts[0].replace('.', '/')
		String artifact = parts[1]
		String version = parts[2]
		return "$group/$artifact/$version/$artifact-${version}.jar"
	}
}

class FileJar extends JarLocation{
	FileJar(URI uri){
		super(uri)
	}

	String getFilename() {
		int inZipPos = schemePart.lastIndexOf('!')
		int fileBegin = schemePart.lastIndexOf('/')
		if(inZipPos!=-1) {
			return schemePart.substring(inZipPos+1)
		}
		else {
			return schemePart.substring(fileBegin+1)
		}
	}

	URL getURL(){
		return new URL(schemePart)
	}
}
