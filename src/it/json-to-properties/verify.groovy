File propsFile = new File(basedir, "target/classes/SLING-INF/app-root/i18n/en.properties")
assert propsFile.exists();

Properties props = new Properties()
propsFile.withInputStream {
    props.load(it)
}

assert props["key1"] == "value1"
assert props["key21.key22.key23"] == "value 2"

return true;
