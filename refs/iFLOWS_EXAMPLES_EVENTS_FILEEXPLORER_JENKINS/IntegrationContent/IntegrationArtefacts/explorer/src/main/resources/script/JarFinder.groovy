package src.main.resources.script

import src.main.resources.script.ZipArchive

class JarFinder {
	String mavenRepositoryURL
	
	JarLocation getLocation(String location) {
		String scheme = location.find('^\\w+:')
		switch(scheme) {
			case 'jar:':
				return new FileJar(new URI(location))
			case 'mvn:':
				MavenJar mvn = new MavenJar(new URI(location))
				mvn.setRepositoryURL(mavenRepositoryURL) 
				return mvn
			default:
				return new UnknownLocation(new URI('unknown:'+location.replace(' ', '_')))
		}
	}
}
