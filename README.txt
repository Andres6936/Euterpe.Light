-----------------------------------------------------
                  JLayerME 0.1.3

 Project Homepage :
   http://www.javazoom.net/javalayer/javalayerme.html 

 JAVA and MP3 online Forums :
   http://www.javazoom.net/services/forums/index.jsp
-----------------------------------------------------

DESCRIPTION :
-----------
JLayerME is a JAVA library that decodes/plays/converts MPEG 1/2 Layer 3 ONLY.
JLayerME MP3 engine is light and fast, twice fast than JLayer classic engine.
JLayerME is oriented for J2ME (CDC/CVM, CLDC/KVM) platforms and portable devices.
This is a non-commercial project and anyone can add his contribution.
JLayerME is licensed under LGPL (see LICENSE.txt).


FAQ : 
---

- How to install JLayerME ?
  Before running JLayerME you must set PATH and CLASSPATH for JAVA
  and you must add jlme0.1.3.jar to the CLASSPATH.

- Do I need JMF to run JLayerME player ?
  No, JMF is not required. You just need a JVM JavaSound 1.0 compliant.
  (i.e. JVM1.3 or higher).

- How to run the simple MP3 player ?
  java -jar jlme0.1.3.jar localfile.mp3
   or
  java -jar jlme0.1.3.jar -url http://www.aserver.com/remotefile.mp3

- Does simple MP3 player support streaming ?
  Yes, use the following command to play music from stream :
  java javazoom.jlme.util.Player -url http://www.shoutcastserver.com:8000

- Does JLayerME support MPEG 2.5 ?
  No, JLayerME supports only MPEG 1/2 Layer 3.
  If you need MPEG 2.5 support then use JLayer classic.

- How to get ID3v1 or ID3v2 tags from JLayerME API ?
  The API doesn't provide this feature. Use JLayer classic instead.


KNOWN PROBLEMS : 
--------------
