package src.main.resources.script

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ZipArchive {
	String filePath
	String archivePath

	ZipArchive(String fullpath){
		int inArchivePos = fullpath.indexOf('!')
		filePath = fullpath.substring(0,inArchivePos)
		archivePath = fullpath.substring(inArchivePos+1)
	}

	String getEntryName() {
		int lastDirPos = archivePath.lastIndexOf('/')
		if(lastDirPos > 0)
			return archivePath.substring(lastDirPos+1)
		int lastEntryPos = archivePath.lastIndexOf('!')
		if(lastEntryPos > 0)
			return archivePath.substring(lastEntryPos+1)
		return archivePath
	}

	InputStream getInputStream() throws IOException {
		ZipInputStream zis = new ZipInputStream(new File(filePath).newInputStream())
		String[] parts = archivePath.split('!')
		ZipEntry e
		for (int i=0;i<parts.length;i++) {
			zis = goToZipEntry(zis,parts[i])
			if(!zis) {
				throw new IOException("Path $archivePath not found in archive $filePath")
			}
			if(i<parts.length-1) {
				zis = new ZipInputStream(zis);
			}
		}

		return zis
	}

	private InputStream goToZipEntry(ZipInputStream is, String name) {
		ZipEntry e

		while(e = is.getNextEntry()) {
			if(e.name==name) return is
		}
		return null
	}
}