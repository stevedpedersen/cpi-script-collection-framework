//original from Martin Pankraz and modified by Dominic Beckbauer
//

import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
       def map = message.getHeaders();
      
      
        def agency1 = map.get("agency1").toString();
        def agency2 = map.get("agency2").toString();
        def identifier1 = map.get("identifier1").toString();
        def identifier2 = map.get("identifier2").toString();       
        
        
         def encoding = message.getBody(java.lang.String);

		def stringPerCode = [:];
		/***********************
			Cheat sheet for pre-processing of CSV:
			([\s]{2,})(?=[aA-zZ])
			([_]{2,})
			
		*/
		encoding.splitEachLine(";") {fields ->
			def listOfItems = [];
			
			fields[1] = fields[1].toString().trim();
			if(fields[1].equals("")){
				fields[1] = "Unbekannt";
			}
			if(fields[1].toString().length() > 60){
				fields[1] = fields[1].substring(0,60);
			}
			fields[1] = fields[1].toString().replaceAll(/[\s]{2,}/,' ');
			
			if(stringPerCode.containsKey(fields[1])){
				def alreadyListed = stringPerCode.get(fields[1]).contains(fields[0]);
				if(!alreadyListed){
					stringPerCode.get(fields[1]) << fields[0];
				} 
			}else{
				listOfItems << fields[0];
				stringPerCode[fields[1]] = listOfItems;
			}
		}
			
		def stringWriter = new StringWriter();
		def mappingBuilder = new MarkupBuilder(stringWriter);
		
		mappingBuilder.mkp.xmlDeclaration(version: "1.0", encoding: encoding)
		
		mappingBuilder.root{
			vm(version:'2.0'){
				stringPerCode.each{code, strings ->
					strings.eachWithIndex { item, index ->
						group(id:UUID.randomUUID().toString()) {
							if(index == 0){
								entry(isDefault:'true') {
									agency(agency1)
									schema(identifier1)
									value(item)
								}
								entry() {
									agency(agency2)
									schema(identifier2)
									value(code)
								}
							}else{
								entry{
									agency(agency1)
									schema(identifier1)
									value(item)
								}
								entry{
									agency(agency2)
									schema(identifier2)
									value(code)
								}
							}
						}
					}
				}
			}
		};
		
		
		message.setBody(stringWriter.toString());
    
    return message;
}