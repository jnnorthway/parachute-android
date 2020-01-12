import os
import socket

class udpTools():
    def __init__(self):
        self.ACK_MSG = b'<ACK>'
        self.EOF_MSG = b'<EOF>'
        self.encoding = 'utf-8'
        self.server_data = {
            "address": ("127.0.0.1", 20001),
            "buffer": 16384
        }
        self.UDPSocket = None
        self.file = None
        self.file_name = None
        self.file_size = 0
        self.msg_sent = 0
        self.msg_received = 0

 
    def createUpdSocket(self):
        if self.UDPSocket is None:
            print("Creating socket.")
            self.UDPSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)


    def printFileInfo(self):
        self.fileInfo(self.file)
        print("file path: %s" % self.file)
        print("file size: %d bytes" % self.file_size)


    def fileInfo(self, file_path):
        self.file = file_path
        self.fileName()
        self.file_stats = os.stat(self.file)
        self.file_size = self.file_stats.st_size


    def fileName(self):
        self.file_name = os.path.split(self.file)[1]

    
    def recieveData(self):
        assert self.UDPSocket, "No socket available."
        data = self.UDPSocket.recvfrom(self.server_data['buffer'])
        self.msg_received += 1
        return data


    def decode(self, data):
        return data.decode(self.encoding)


    def close(self):
        self.UDPSocket.close()


class updClient(udpTools):
    def __init__(self, file_path):
        super().__init__()
        self.fileInfo(file_path)


    def sendData(self, data):
        assert self.UDPSocket, "No socket available."
        if isinstance(data, str):
            data = str.encode(data)
        assert isinstance(data, bytes), "data not in byte form."
        self.UDPSocket.sendto(data, self.server_data['address'])
        self.msg_sent += 1


    def sendFile(self):
        self.createUpdSocket()
        print("sending file: %s" % self.file)
        self.sendData(self.file_name)
        f = open(self.file, "rb")
        data = f.read(self.server_data['buffer'])
        while data:
            # Send to server using created UDP socket
            self.sendData(data)
            # print_send(num_messages)

            msgFromServer = self.recieveData()[0]
            assert msgFromServer == self.ACK_MSG, "Something went wrong, \"%s\"" % self.decode(msgFromServer)
            data = f.read(self.server_data['buffer'])
        f.close()
        self.sendData(self.EOF_MSG)
        print("file sent.")


class updServer(udpTools):
    def __init__(self):
        self.resource_path = 'resources'
        super().__init__()

    def sendData(self, data, address):
        if isinstance(data, str):
            data = str.encode(data)
        assert isinstance(data, bytes), "data not in byte form."
        self.UDPSocket.sendto(data, address)
        self.msg_sent += 1

    def receiveFile(self):
        # Listen for incoming datagrams
        message = None
        data = b''
        self.UDPSocket.bind(self.server_data['address'])
        while(self.EOF_MSG != message):
            bytesAddressPair = self.recieveData()
            message = bytesAddressPair[0]
            address = bytesAddressPair[1]
            # Sending a reply to client
            self.sendData(self.ACK_MSG, address)
            if self.file is None:
                self.file = os.path.join(self.resource_path, self.decode(message))
                print("receiving file: %s" % self.file)
            else:
                data += message
        data = data.strip(self.EOF_MSG)
        f=open(self.file, "wb")
        f.write(data)
        print("File written: %s" % self.file)
        f.close()
        self.file_name = None
