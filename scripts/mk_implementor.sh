impl_path="info/kgeorgiy/ja/fedorenko/implementor"
javac -cp ../../pull/artifacts/info.kgeorgiy.java.advanced.implementor.jar ../java-solutions/${impl_path}/Implementor.java
jar -cvfm implementor.jar ./MANIFEST.MF ${impl_path}/Implementor.class
