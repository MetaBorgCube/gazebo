from xml.etree import ElementTree


def read_pom_vers(pom_path):
    pom = ElementTree.parse(pom_path).getroot()
    pom_props = pom.find("{http://maven.apache.org/POM/4.0.0}properties[1]")
    pom_revision = pom_props.find("{http://maven.apache.org/POM/4.0.0}revision").text
    pom_mb_ver = pom_props.find("{http://maven.apache.org/POM/4.0.0}metaborgVersion").text
    return pom_revision or "0.1.0-SNAPSHOT", pom_mb_ver or "2.6.0-SNAPSHOT"
