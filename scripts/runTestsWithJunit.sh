lib_path="../lib"
classpath="${lib_path}/junit-4.11.jar:${lib_path}/assertj-core-3.24.2.jar:${lib_path}/hamcrest-core-1.3.jar"
bank_path="../java-solutions/info/kgeorgiy/ja/fedorenko/bank"
javac -cp ${classpath} -d ./out ${bank_path}/src/*.java ${bank_path}/app/*.java  ${bank_path}/test/*.java 
java -classpath ${classpath}:./out org.junit.runner.JUnitCore info.kgeorgiy.ja.fedorenko.bank.test.AppTests
rm -rf ./out
