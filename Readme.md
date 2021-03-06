# Euterpe.Light

Project [Homepage](http://www.javazoom.net/javalayer/javalayerme.html)

Java and MP3 online [Forums](http://www.javazoom.net/services/forums/index.jsp)

---

### Description

- Euterpe.Light is a Java library that decodes/plays/converts MPEG 1/2 Layer 3 ONLY.
- Euterpe.Light MP3 engine is light and fast, twice fast than JLayer classic engine.
- Euterpe.Light is oriented for J2ME (CDC/CVM, CLDC/KVM) platforms and portable devices.
- This is a non-commercial project and anyone can add his contribution.
- Euterpe.Light is licensed under LGPL (see [License.md](/License.md)).

### FAQ

- How to install Euterpe.Light? <br>
  Before running Euterpe.Light you must set PATH and CLASSPATH for JAVA and you must add jlme0.1.3.jar to the CLASSPATH.

- Do I need JMF to run Euterpe.Light player? <br>
  No, JMF is not required. You just need a JVM JavaSound 1.0 compliant.
  (i.e. JVM1.3 or higher).

- How to run the simple MP3 player? <br>
  `java -jar jlme0.1.3.jar localfile.mp3`
  <br>or<br>
  `java -jar jlme0.1.3.jar -url http://www.aserver.com/remotefile.mp3`

- Does simple MP3 player support streaming? <br>
  Yes, use the following command to play music from stream :
  `java javazoom.jlme.util.Player -url http://www.shoutcastserver.com:8000`

- Does Euterpe.Light support MPEG 2.5? <br>
  No, Euterpe.Light supports only MPEG 1/2 Layer 3. If you need MPEG 2.5 support then use JLayer classic.

- How to get ID3v1 or ID3v2 tags from Euterpe.Light API? <br>
  The API doesn't provide this feature. Use JLayer classic instead.

### Run with JetBrains IDE

Is important to enable the assertions

- VM Options: -ea:$FileDirRelativeToSourcepath$...