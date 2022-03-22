import frida
import time
from hookez import socketClient


socket_aller = '/workdir/socket_p2j'
socket_retour = '/workdir/socket_j2p'




class State(object):
    def __init__(self, args):
        self.main_attached = False
        self.main_session = ""
        self.sessions_list = []
        self.cmds_count = 0
        self.address = ""
        self.jdiPort = ""
        self.fridaPort = ""
        self.socketClient = socketClient.SocketClient(socket_aller, socket_retour, self)
        if args.address is not None and args.jdiport is not None:
            self.address = args.address
            self.jdiPort = int(args.jdiport)
        else:
            print("You must at least provide the address and the port of the machine with the running JVM")
            exit(1)
        if args.fridaport is not None:
            try:
                self.fridaPort = args.fridaport
                d = frida.get_device_manager().add_remote_device(self.address + ":" + str(self.fridaPort))
                self.setMainSession(d.attach('java'))
            except:
                print("Frida direct connection to main session failed")
                print("If the main JVM is on a Windows machine, it's okay")
                print("Frida is not attached to the main JVM, expect limited functionnality")
        else:
            print("Frida is not attached to the main JVM, expect limited functionnality")
    
    def setMainSession(self, main_s):
        self.main_attached = True
        self.main_session = main_s
    
    def mainAttached(self):
        return self.main_attached
    
    def getMainSession(self):
        if self.mainAttached():
            return self.main_session
        else:
            return None

    def loadConfig(self, filepath, commandline):
        with open(filepath, 'r') as f:
            for line in f.readlines():
                if line.startswith('#'):
                    line = ""
                line = commandline.precmd(line)
                commandline.onecmd(line)
        return

    def stopDebugger(self):
        self.socketClient.send_async({"cmd": "stop_tasks", "args": []})

    def addSession(self, st):
        self.sessions_list.append(st)
        print("--> New session : " + st["stacktrace"]["method"] + ", Session id : " + str(st["stacktrace"]["id"]))
    
    def getSessionsList(self):
        self.sessions_list.sort(key=lambda x : x["time"])
        return self.sessions_list
    
    def sessionExists(self, sess_id):
        return sess_id in [x["stacktrace"]["id"] for x in self.sessions_list]
    
    def runScript(self, script_str):
        if self.mainAttached():
            x = self.getMainSession().create_script(script_str)
            x.load()

    def wait_cmds(self):
        while self.cmds_count > 0:
            time.sleep(0.1)
