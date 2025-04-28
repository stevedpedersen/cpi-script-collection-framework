package com.tm
import groovy.text.SimpleTemplateEngine
import groovy.json.*

class ValueMAppingJSONExampleTM {
    static String getId() {
        return UUID.randomUUID().toString().replaceAll('-', '')
    }
    static String createMappingJSON() {
        def mappingMetadata = [id: getId(), sourceAgency: "C4C", sourceIdentifier: "ServiceCat", targetAgency: "S4", targetIdentifier: "DocType"]
        def mappings = [
            [id: getId(), parentId: mappingMetadata.id, sourceValue: "C4C1", targetValue: "S41", targetDefault: true],
[id: getId(), parentId: mappingMetadata.id, sourceValue: "C4C2", targetValue: "S442"]
        ]
        JsonBuilder builder = new JsonBuilder()
        builder {
            version "2.0"
            valueMappingCLMS ([
                {
                    id mappingMetadata.id
                    isConfigured false
                    cvmList mappings.collect {
                        mapping -> [
                            "id": mapping.id,
                            "parentId": mapping.parentId,
                            "valueMappingEntryList": [
                                {
                                    value  mapping.sourceValue
                                    isDefault mapping.sourceDefault
                                },
                                {
                                    value  mapping.targetValue
                                    isDefault mapping.targetDefault
                                }
                            ]
                        ]
                    }
                    source {
                        agency mappingMetadata.sourceAgency
                        schema mappingMetadata.sourceIdentifier
                    }
                    target {
                        agency mappingMetadata.targetAgency
                        schema mappingMetadata.targetIdentifier
                    }
                }
            ])
        }
        return JsonOutput.prettyPrint(builder.toString())
    }
    static void main(def args) {
        println createMappingJSON()
    }
}