import socket
import json
import time
import threading
import base64

class SocketClient():
    def __init__(self, sock_path_aller, sock_path_retour, state):        
        self.state = state
        self.sock_aller = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock_retour = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        try:
            self.sock_aller.connect(sock_path_aller)
            self.sock_retour.connect(sock_path_retour)
        except:
            print("Cannot connect to debugger socket")
            exit(1)
        self.request_list = []
        self.response_list = []
        self.recv_loop()
    
    def send_sync(self, cmd, response_count=1):
        """
        cmd is a dict {"cmd":"xyz", "args":[1,2,3]}
        """
        # send {"id":X, "cmd":X, "args":[1,2,3]}
        id = self.send_async(cmd)
        while len(self.response_list[id]) < response_count:
            time.sleep(0.001)
        if response_count == 1:
            return self.response_list[id][0]
        return self.response_list[id]
    
    def send_async(self, cmd):
        id = len(self.request_list)
        cmd_dict = {"id": id, "cmd": cmd["cmd"], "args": self.encode_args(cmd["args"])}
        sendcmd = json.dumps(cmd_dict)
        sendcmd = str(len(sendcmd)) + "_" + sendcmd
        self.request_list.append(cmd_dict)
        self.response_list.append([])
        self.sock_aller.send(sendcmd.encode())
        return id
    
    def get_response_sync(self, id, response_count=1):
        while len(self.response_list[id]) < response_count:
            time.sleep(0.001)
        if response_count == 1:
            return self.response_list[id][0]
        return self.response_list[id]
    
    def recv_loop(self):
        # Launch an other thread and listen on the socket
        def extractPacket(m):
            nb_len = m.find('_')
            if nb_len < 1:
                return m
            l = int(m[:nb_len], 10)
            if len(m) >= l + nb_len + 1:
                inner = json.loads(m[nb_len + 1: nb_len + 1 + l])
                if inner["id"] == -1:
                    # On a reçu une session !
                    sess = json.loads(base64.b64decode(inner["msg"]).decode())
                    self.state.addSession(sess)
                elif inner["id"] == -2:
                    # On a reçu le retour d'une commande passée
                    used_cmd = base64.b64decode(inner["msg"]).decode()
                    print("The command :\n\t" + used_cmd + "\nended")
                    self.state.cmds_count -= 1
                else:
                    self.response_list[inner["id"]].append(inner["msg"])
                return m[nb_len + 1 + l:]
            return m

        def inner_func():
            infinite_buffer = ""
            while 1:
                infinite_buffer += self.sock_retour.recv(1024).decode()
                new_infinite_buffer = extractPacket(infinite_buffer)
                while infinite_buffer != new_infinite_buffer:
                    infinite_buffer = new_infinite_buffer
                    new_infinite_buffer = extractPacket(infinite_buffer)

        x = threading.Thread(target=inner_func)
        x.start()
    
    def encode_args(self, cmd):
        return [json.dumps({'val':x}) for x in cmd]
