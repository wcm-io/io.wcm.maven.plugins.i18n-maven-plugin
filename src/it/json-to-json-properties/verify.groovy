import groovy.json.JsonSlurper

File jsonFile = new File(basedir, "target/classes/SLING-INF/app-root/i18n/en.json")
assert jsonFile.exists();

def json = new JsonSlurper().parseText(jsonFile.getText("utf-8"))

assert json["key1"] == "value1"
assert json["key21.key22.key23"] == "value 2"

return true;
