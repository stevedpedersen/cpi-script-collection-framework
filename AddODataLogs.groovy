import org.apache.olingo.odata2.api.uri.UriInfo
import com.sap.gateway.ip.core.customdev.logging.*


/*
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import org.apache.olingo.odata2.api.uri.UriInfo;
import com.sap.gateway.ip.core.customdev.logging.*; 
def Message processData(Message message) {
	def uriInfo = message.getHeaders().get("UriInfo");	
	def funcImpParams = uriInfo.getFunctionImportParameters();
	if(funcImpParams  != null && !funcImpParams.isEmpty()){
	    log.logErrors(LogMessage.TechnicalError, "FunctionImport"+funcImpParams);
	    def k=0;
		for(item in funcImpParams)
		{
			log.logErrors(LogMessage.TechnicalError, "Functionimport Param "+(++k)+" : "+ item.getKey()+" = "+item.getValue().getLiteral());
			message.setHeader(item.getKey(),item.getValue().getLiteral());
		}
	}
	return message;
}
*/

def Message processData(Message message) {
  def odataURI = new StringBuilder()
def keyPredList = uriInfo.getKeyPredicates();				
def k=0;
for(item in keyPredList)
{
log.logErrors(LogMessage.TechnicalError, (++k) + " Key Predicate value for property "+item.getProperty().getName()+" is: "+ item.getLiteral());
message.setHeader("Key_"+item.getProperty().getName(),item.getLiteral());
}	
def targetEntityName = uriInfo.getTargetEntitySet().getName();
def startEntityName = uriInfo.getStartEntitySet().getName();	
//Handle Navigation request
if(!targetEntityName.equals(startEntityName)){
log.logErrors(LogMessage.TechnicalError, "Navigation request from source "+ startEntityName + " to target " +targetEntityName);
//Do your own processing to decide which Supplier ID to retrive.
message.setHeader("SupplierID","0");
}else{
    //Handle SupplierSet Read
}
  // Reference for uriInfo:com.sap.gateway.core.ip.provider.data.UriInfoImpl.java
  def urlDelim = "&"
  def urlConcat = "?"
  def entityName = uriInfo.getTargetEntitySet().getName()
  log.logErrors(LogMessage.TechnicalError, "Entity Name::${entityName}")
  if (uriInfo.getTop != null) {
    def top = uriInfo.getTop()
    if (odataURI.size() != 0) {
      odataURI.append(urlDelim)
    }
    odataURI.append("\$top=").append(top)
    log.logErrors(LogMessage.TechnicalError, "Top Value::${top}")
  }
  if (uriInfo.getSkip() != null) {
    def skip = uriInfo.getSkip()
    if (odataURI.size() != 0) {
      odataURI.append(urlDelim)
    }
    odataURI.append("\$skip=").append(skip)
    log.logErrors(LogMessage.TechnicalError, "Skip Value::${skip}")
  }
  if (uriInfo.getFilter() != null) {
    def filterValue = uriInfo.getFilter().getUriLiteral();
    filterValue = filterValue.replace("ProductID","ID");  //The receiver has property names as ID and not ProductID
    if(odataURI.size()!=0) {
      odataURI.append(urlDelimiter);
    }
    odataURI.append("\$filter=").append(filterValue);
    log.logErrors(LogMessage.TechnicalError, "Filter value: "+filterValue);
  }
  if(uriInfo.getExpand() != null){
      def expandList = uriInfo.getExpand();
      def expandValue;
      log.logErrors(LogMessage.TechnicalError, "expandList size: "+expandList.size());
      if(expandList.size()!=0){
      odataURI.append(urlDelimiter);
        for(item in expandList){
            if(item.size() > 0){
                for(navSegments in item){
                      expandValue = navSegments.getNavigationProperty().getName();  //TO DO : Multiple expand values to be handled
                }
            }
        }
        odataURI.append("\$expand=").append(expandValue);
          log.logErrors(LogMessage.TechnicalError, "expand value: "+expandValue);
      }
  }
  log.logErrors(LogMessage.TechnicalError, "URI Value::${odataURI.toString()}")
  message.setHeader("odataEntity", entityName)
  message.setHeader("odataURI", odataURI.toString())

  return message
}