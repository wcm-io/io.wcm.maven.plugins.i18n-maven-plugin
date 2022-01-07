import groovy.util.XmlSlurper

File xmlFile = new File(basedir, "target/classes/SLING-INF/app-root/i18n/en.xml")
assert xmlFile.exists();

def xml = new XmlSlurper().parseText(xmlFile.getText("utf-8"))

assert xml["key1"]["@sling:message"] == "value1"
assert xml["key21.key22.key23"]["@sling:message"] == "value 2"

return true;
