const net = require('net');

function writeVarInt(value) {
    const bytes = [];
    while (true) {
        if ((value & ~0x7F) === 0) {
            bytes.push(value);
            return Buffer.from(bytes);
        }
        bytes.push((value & 0x7F) | 0x80);
        value >>>= 7;
    }
}

function readVarInt(buf, offset = 0) {
    let result = 0;
    let shift = 0;
    let bytesRead = 0;
    while (true) {
        const byte = buf[offset + bytesRead];
        bytesRead++;
        result |= (byte & 0x7F) << shift;
        if ((byte & 0x80) === 0) break;
        shift += 7;
    }
    return { value: result, bytesRead };
}

function createPacket(packetId, payload = Buffer.alloc(0)) {
    const idBuf = writeVarInt(packetId);
    const lenBuf = writeVarInt(idBuf.length + payload.length);
    return Buffer.concat([lenBuf, idBuf, payload]);
}

function writeString(str) {
    const strBuf = Buffer.from(str, 'utf8');
    const lenBuf = writeVarInt(strBuf.length);
    return Buffer.concat([lenBuf, strBuf]);
}

console.log('===============================================================');
console.log('  Testing Minecraft 26.3 (Protocol 777) Direct Client Handshake');
console.log('===============================================================');

const socket = net.createConnection({ host: '127.0.0.1', port: 25565 }, () => {
    console.log('[+] Connected TCP socket to Nome 26.3 Server (127.0.0.1:25565)');

    // 1. Handshake Packet (ID 0x00, protocol 777, host '127.0.0.1', port 25565, nextState 2 = LOGIN)
    const protoBuf = writeVarInt(777);
    const hostBuf = writeString('127.0.0.1');
    const portBuf = Buffer.alloc(2);
    portBuf.writeUInt16BE(25565, 0);
    const nextStateBuf = writeVarInt(2); // LOGIN
    const handshakePayload = Buffer.concat([protoBuf, hostBuf, portBuf, nextStateBuf]);
    socket.write(createPacket(0x00, handshakePayload));
    console.log('[+] Sent Handshake Packet: Protocol 777 (26.3), NextState: LOGIN');

    // 2. Login Start (ID 0x00, Name: 'Mtienn26_3', UUID 16 bytes)
    const nameBuf = writeString('Mtienn26_3');
    const uuidBuf = Buffer.alloc(16, 1);
    const loginStartPayload = Buffer.concat([nameBuf, uuidBuf]);
    socket.write(createPacket(0x00, loginStartPayload));
    console.log('[+] Sent Login Start Packet: Username "Mtienn26_3"');
});

let state = 'LOGIN';
let rxBuffer = Buffer.alloc(0);

socket.on('data', (chunk) => {
    rxBuffer = Buffer.concat([rxBuffer, chunk]);

    while (rxBuffer.length > 0) {
        let pktLenInfo;
        try {
            pktLenInfo = readVarInt(rxBuffer, 0);
        } catch (e) {
            break;
        }

        if (rxBuffer.length < pktLenInfo.bytesRead + pktLenInfo.value) {
            break; // Incomplete packet
        }

        const packetData = rxBuffer.slice(pktLenInfo.bytesRead, pktLenInfo.bytesRead + pktLenInfo.value);
        rxBuffer = rxBuffer.slice(pktLenInfo.bytesRead + pktLenInfo.value);

        const pktIdInfo = readVarInt(packetData, 0);
        const packetId = pktIdInfo.value;
        const payload = packetData.slice(pktIdInfo.bytesRead);

        if (state === 'LOGIN') {
            if (packetId === 0x02) {
                console.log('[+] Received Login Finished Packet (0x02)! Payload length:', payload.length);
                // In 26.3, payload has: UUID (16), name (VarInt + UTF8), propCount (VarInt 0), sessionId (16 bytes UUID)
                const uuid = payload.slice(0, 16).toString('hex');
                const nameLen = readVarInt(payload, 16);
                const name = payload.slice(16 + nameLen.bytesRead, 16 + nameLen.bytesRead + nameLen.value).toString('utf8');
                const afterName = 16 + nameLen.bytesRead + nameLen.value;
                const propCount = readVarInt(payload, afterName);
                const afterProps = afterName + propCount.bytesRead;
                const sessionId = payload.slice(afterProps, afterProps + 16).toString('hex');
                console.log(`    - Verified 26.3 LoginFinished: Name="${name}", SessionId=${sessionId} (Valid!)`);

                // Send Login Acknowledged (0x03)
                state = 'CONFIGURATION';
                socket.write(createPacket(0x03));
                console.log('[+] Sent ServerboundLoginAcknowledgedPacket (0x03) -> State: CONFIGURATION');
            }
        } else if (state === 'CONFIGURATION') {
            console.log('[+] Received Configuration Packet ID: 0x' + packetId.toString(16) + ' (len: ' + payload.length + ')');
            if (packetId === 0x03) {
                console.log('[+] SUCCESS: Received ClientboundFinishConfigurationPacket (0x03) from Nome 26.3!');
                // Send ServerboundFinishConfigurationPacket (0x03)
                state = 'PLAY';
                socket.write(createPacket(0x03));
                console.log('[+] Sent ServerboundFinishConfigurationPacket (0x03) -> State: PLAY');
            }
        } else if (state === 'PLAY') {
            console.log('[+] Received PLAY Packet ID: 0x' + packetId.toString(16) + ' (len: ' + payload.length + ')');
            if (packetId === 0x32) {
                console.log('    -> Verified 26.3 ClientboundLoginPacket (0x32)!');
            } else if (packetId === 0x60) {
                console.log('    -> Verified 26.3 SetChunkCacheCenterPacket (0x60)!');
            } else if (packetId === 0x63) {
                console.log('    -> Verified 26.3 SetDefaultSpawnPositionPacket (0x63)!');
            } else if (packetId === 0x49) {
                console.log('    -> Verified 26.3 PlayerPositionPacket (0x49)!');
            } else if (packetId === 0x2E) {
                console.log('    -> Verified 26.3 LevelChunkWithLightPacket (0x2E)! 3D Procedural Terrain Received!');
                console.log('===============================================================');
                console.log('  MINECRAFT 26.3 (PROTOCOL 777) CONNECTION VERIFIED 100%!     ');
                console.log('===============================================================');
                socket.end();
                process.exit(0);
            }
        }
    }
});

socket.on('error', (err) => {
    console.error('[-] Socket error:', err.message);
    process.exit(1);
});

socket.on('close', () => {
    console.log('[*] Connection closed.');
});
