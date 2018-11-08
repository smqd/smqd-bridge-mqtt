/*
	@Begin
	  send test message to 'sensor/123/humidity'
	@End
 */

import com.thing2x.smqd.Smqd
import com.thing2x.smqd.net.telnet.ScShell

val args: Array[String] = $args
val shell: ScShell = $shell
val smqd: Smqd = shell.smqd

smqd.publish("sensor/123/humidity", """{"command": "test", "msg": "Hello World", "num": 456}""")
