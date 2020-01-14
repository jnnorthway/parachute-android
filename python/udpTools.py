import os
import sys
import time
import socket


class udpTools():
    def __init__(self):
        self.ACK_MSG = b'<ACK>'
        self.EOF_MSG = b'<EOF>'
        self.timeout = 3.0
        self.max_attempts = 3
        self.encoding = 'utf-8'
        self.server_data = {
            "address": ("192.168.56.1", 20001),
            # "address": ("127.0.0.1", 20001),
            # "buffer": 8192
            "buffer": 1024
        }
        self.UDPSocket = None
        self.file = None
        self.file_name = None
        self.file_size = 0
        self.msg_sent = 0
        self.msg_received = 0


    def printProgress(self):
        progress_str = "\rProgress: [%s] %s"
        columns = int(os.popen('stty size', 'r').read().split()[1])
        columns -= len(progress_str)
        size_received = self.msg_received * self.server_data['buffer']
        percentage = min(1, size_received / self.file_size)
        percent_buffer = ''
        for i in range(columns):
            if (percentage * columns) >= i:
                percent_buffer += '#'
            else:
                percent_buffer += ' '
        format_percent = "{0:.0%}".format(percentage)
        sys.stdout.write(progress_str % (percent_buffer, format_percent))


    def clearProgress(self):
        columns = int(os.popen('stty size', 'r').read().split()[1])
        buffer = '\r'
        for _ in range(columns):
            buffer += ' '
        buffer += '\r'
        sys.stdout.write(buffer)

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
        if self.file_size == 0:
            self.file_stats = os.stat(self.file)
            self.file_size = self.file_stats.st_size


    def fileName(self):
        self.file_name = os.path.split(self.file)[1]

    
    def recieveData(self):
        assert self.UDPSocket, "No socket available."
        try:
            data = self.UDPSocket.recvfrom(self.server_data['buffer'])
        except:
            return None
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


    def sendRawData(self, data):
        assert self.UDPSocket, "No socket available."
        if isinstance(data, str):
            data = str.encode(data)
        assert isinstance(data, bytes), "data not in byte form."
        self.UDPSocket.sendto(data, self.server_data['address'])


    def sendData(self, data, attempt=0):
        assert self.UDPSocket, "No socket available."
        assert attempt < self.max_attempts
        if isinstance(data, str):
            data = str.encode(data)
        assert isinstance(data, bytes), "data not in byte form."
        self.UDPSocket.sendto(data, self.server_data['address'])
        msgFromServer = self.recieveData()
        if msgFromServer == None:
            attempt += 1
            self.clearProgress()
            print("Failed to receive, trying again: attempt = %d" % attempt)
            return self.sendData(data, attempt)
        msgFromServer = msgFromServer[0]
        assert msgFromServer == self.ACK_MSG, "Something went wrong, \"%s\"" % self.decode(msgFromServer)
        self.msg_sent += 1
        return msgFromServer


    def sendFile(self):
        start_time = time.time()
        self.createUpdSocket()
        self.UDPSocket.settimeout(self.timeout)
        print("sending file: %s" % self.file)
        self.sendData(self.file_name)
        self.sendData(str(self.file_size).encode())
        f = open(self.file, "rb")
        data = f.read(self.server_data['buffer'])
        while data:
            # Send to server using created UDP socket
            self.sendData(data)
            self.printProgress()
            
            data = f.read(self.server_data['buffer'])
        f.close()
        self.sendData(self.EOF_MSG)
        print("\nFile sent.")
        print("Send time: %f" % (time.time() - start_time))


class updServer(udpTools):
    def __init__(self):
        self.resource_path = 'resources'
        super().__init__()


    def sendData(self, data, address):
        assert self.UDPSocket, "No socket available."
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
                start_time = time.time()
                print("receiving file: %s" % self.file)
                start_time = time.time()
            elif self.file_size == 0:
                self.file_size = int(self.decode(message))
                self.printFileInfo()
            else:
                data += message
                self.printProgress()

        data = data.strip(self.EOF_MSG)
        f=open(self.file, "wb")
        f.write(data)
        print("\nFile written: %s" % self.file)
        f.close()
        print("Receive time: %f" % (time.time() - start_time))
        self.file_name = None
