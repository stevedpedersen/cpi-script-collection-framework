/*Copyright (c) 2020 INDEVO, Jacek Kopcinski
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.*/

package src.main.resources.script

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil

import com.sap.gateway.ip.core.customdev.util.Message

import groovy.io.FileType
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.Field
import groovy.xml.MarkupBuilder

@Field String iflowURL
@Field String action
@Field Map params
@Field Map headers
@Field SimpleTemplateEngine engine = new SimpleTemplateEngine()
@Field JarFinder jarFinder = new JarFinder()

//Entry Point
def Message processData(Message message) {
	headers = message.headers
	params = getQueryParams( headers.get("CamelHttpQuery") )
	iflowURL = getIflowURL(message)
	jarFinder.setMavenRepositoryURL(message.properties.mavenRepositoryURL)

	action = headers.CamelHttpPath
	switch(action) {
		case "bundles":
			renderBundles(message)
			return message
		case "bundle":
			renderBundle(message)
			return message
		case "files":
			renderFiles(message)
			return message
		case "download":
			download(message)
			return message
		case "classes":
			renderClasses(message)
			return message
		default:
			renderSystem(message)
			return message
	}
}

Template readTemplate(String templateName) {
	String templatePath = '/src/main/resources/templates/' + templateName
	InputStream is = this.class.classLoader.getResourceAsStream(templatePath)
	return engine.createTemplate(new InputStreamReader(is,"UTF-8"))
}

def void renderMain(Message message, Map binding) {
	def content = readTemplate('main.gsp').make(binding)

	message.headers["Content-Type"] = "text/html"
	message.setBody(content)
}

def void renderSystem(Message message) {
	List bundles = findBundles(~/org\.apache\.camel\.camel-core/)
	
	Map binding = new HashMap()
	if(bundles)
		binding["camelVersion"] = bundles.first().version
	else
		binding["camelVersion"] = "n/a"

	def content = readTemplate('system.gsp').make(binding)

	binding = new HashMap()
	binding["action"] = action
	binding["pageTitle"] = "System Information"
	binding["content"] = content


	renderMain(message,binding)
}

def renderBundles(Message message) {
	Pattern pattern
	if(params.name) {
		pattern = patternToRegex(params.name)
	}

	List bundles
	if(pattern) {
		bundles = findBundles(pattern)
	}

	Map binding = new HashMap()
	binding["namePattern"] = params.name
	binding["bundles"] = bundles
	def content = readTemplate('bundles.gsp').make(binding)

	binding = new HashMap()
	binding["action"] = action
	binding["pageTitle"] = "System Bundles"
	binding["content"] = content

	renderMain(message,binding)
}

def renderBundle(Message message) {
	if(!params.bundleID) {
		message.setBody("Eror: Parameter bundleID is not provided")
		return
	}

	Bundle bundle = getBundleContext().getBundle(Long.parseLong(params.bundleID))
	if(!bundle) {
		message.setBody("Bundle with ID $params.bundleID is not deployed")
		return
	}

	Map binding = new HashMap()
	binding["bundle"] = bundle
	binding["jarLink"] = jarLink(bundle.location)
	binding["resources"] = bundle.getResources("/")
	binding["entries"] = bundle.findEntries("/", "*", true)

	def content = readTemplate('bundle.gsp').make(binding)

	binding = new HashMap()
	binding["action"] = action
	binding["pageTitle"] ="Bundle Details"
	binding["content"] = content

	renderMain(message,binding)
}

def Message renderClasses(Message message) {
	Pattern pattern
	if(params.name) {
		String classPath = '/'+params.name.replace('.','/') + '.class'
		pattern = patternToRegex(classPath)
	}
	List classes
	if(pattern) {
		classes = findClasses(pattern)
	}

	Map binding = new HashMap()
	binding["namePattern"] = params.name
	binding["classes"] = classes
	def content = readTemplate('classes.gsp').make(binding)

	binding = new HashMap()
	binding["action"] = action
	binding["pageTitle"] ="Class Finder"
	binding["content"] = content

	renderMain(message,binding)
}

def renderFiles(Message message) {
	String browseURL = getActionURL("browse")
	String downloadURL = getActionURL("download")
	File dir;
	if(params.dir) {
		dir = new File(params.dir)
	}
	else {
		dir = new File("/");
	}
	if(!dir.exists() || !dir.isDirectory()) {
		message.setBody("Directory $dir.path does not exist");
		return
	}

	Map binding = new HashMap()
	binding["dir"] = dir
	binding["script"] = this
	binding["dateFormat"] = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")

	def content = readTemplate('files.gsp').make(binding)

	binding = new HashMap()
	binding["action"] = action
	binding["pageTitle"] ="File Browser"
	binding["content"] = content

	renderMain(message,binding)
}

void download(Message message) {
	String filePath = params.file;
	if(!filePath) {
		message.setBody("Error: Parameter file is not provided!")
		return
	}
	try {
		Map content = [:]
		if(filePath.contains('!')) {
			ZipArchive archive = new ZipArchive(filePath)
			content.filename = archive.entryName
			content.bytes = archive.inputStream.bytes
		}else {
			File file = new File(filePath)
			content.filename = file.name
			content.bytes = file.newInputStream().bytes
		}
		message.setBody(content.bytes)
		message.headers["Content-Transfer-Encoding"] = "base64"
		message.headers["Content-Disposition"] = "attachment; filename=\"${content.filename}\""
	} catch (IOException e) {
		message.setBody("Error: Cannot open file $filePath")
		return;
	}
}

def List findBundles(Pattern pattern) {
	List bundles = new ArrayList()
	getBundleContext().getBundles().findAll{ it.symbolicName.toLowerCase().matches(pattern) }.each { Bundle b ->
		Map bundle = [:]
		bundle.name = b.getSymbolicName()
		bundle.description = b.getHeaders().get('Bundle-Name')
		bundle.version = b.getVersion()
		bundle.link = bundleLink(b)
		bundles << bundle
	}
	return bundles
}

def List findClasses(Pattern pattern) {
	List classes = new ArrayList()

	getBundleContext().getBundles().each { Bundle b ->
		Enumeration<URL> entries = b.findEntries("/", "*.class", true)
		Collection<URL> urls = entries.findAll { URL u -> u.path.toLowerCase().matches(pattern) }
		urls.each { url ->
			Map c = [:]
			c.name = urlToQClass(url)
			c.bundle = b.symbolicName
			c.bundleLink = bundleLink(b)
			c.jarLink = jarLink(b.location)
			classes << c
		}
	}
	return classes
}

def String urlToQClass(URL u) {
	return u.path.replaceFirst('/','').replace('/', '.').replace('.class', '')
}

def Pattern patternToRegex(String simplePattern) {
	String regex = simplePattern.toLowerCase()
	regex = regex.replace('.', '\\.')
	regex = regex.replace('*', '.*')
	regex = regex.replace('?', '.?')
	regex = regex.replace('$', '\\$')
	Pattern pattern
	try {
		pattern = Pattern.compile(regex)
		return pattern
	} catch (PatternSyntaxException e) {
		return null //Ignore invalid patterns
	}
}

def String jarLink(String uri) {
	JarLocation jarLocation = jarFinder.getLocation(uri)
	if(jarLocation.isUnknown()) {
		return null
	}else {
		if(jarLocation.isLocal()) {
			return downloadLink(jarLocation.URL.path)
		}else {
			return jarLocation.URL
		}
	}
}

def String bundleLink(Bundle bundle) {
	return "${getActionURL('bundle')}?bundleID=$bundle.bundleId"
}

def String directoryLink(String path) {
	return "${getActionURL('files')}?dir=$path"
}

def String downloadLink(String path) {
	return "${getActionURL('download')}?file=$path"
}

def Map getQueryParams(String query) {
	Map map = new HashMap()
	if(query) {
		def parameters = query.split("&")
		parameters.each { it ->
			String[] pair = it.split("=")
			if(pair.length==2) {
				map.put(pair[0], URLDecoder.decode(pair[1],"UTF-8"))
			}
		}
	}
	return map
}

def BundleContext getBundleContext() {
	Bundle msgBundle = FrameworkUtil.getBundle(Message.class)
	if(msgBundle==null) {
		throw new AssertionError("No OSGi bundle for class ${Message.class}")
	}
	return msgBundle.bundleContext
}

def getIflowURL(Message message) {
	return headers.CamelHttpUrl.minus(headers.CamelHttpPath)
}

def getActionURL(String actionName) {
	return iflowURL + actionName;
}
