const net = require('net');

function writeVarInt(value) {
    const bytes = [];
    let val = value >>> 0;
    while (true) {
        if ((val & ~0x7F) === 0) {
            bytes.push(val);
            return Buffer.from(bytes);
        }
        bytes.push((val & 0x7F) | 0x80);
        val >>>= 7;
    }
}

function readVarInt(buf, offset = 0) {
    let result = 0;
    let shift = 0;
    let cursor = offset;
    while (cursor < buf.length) {
        const b = buf[cursor++];
        result |= (b & 0x7F) << shift;
        if ((b & 0x80) === 0) {
            return { value: result, bytesRead: cursor - offset };
        }
        shift += 7;
    }
    return null;
}

function createPacket(packetId, payload = Buffer.alloc(0)) {
    const idBuf = writeVarInt(packetId);
    const lengthBuf = writeVarInt(idBuf.length + payload.length);
    return Buffer.concat([lengthBuf, idBuf, payload]);
}

const client = net.createConnection({ host: '127.0.0.1', port: 25565 }, () => {
    console.log('[Ping] Connected to Nome server');

    // 1. Handshake packet (0x00)
    // Protocol 777 (26.3), Host "127.0.0.1", Port 25565, Next State 1 (Status)
    const protoBuf = writeVarInt(777);
    const hostStr = '127.0.0.1';
    const hostLenBuf = writeVarInt(hostStr.length);
    const hostBuf = Buffer.from(hostStr, 'utf8');
    const portBuf = Buffer.alloc(2);
    portBuf.writeUInt16BE(25565, 0);
    const stateBuf = writeVarInt(1); // 1 = Status

    const handshakePayload = Buffer.concat([protoBuf, hostLenBuf, hostBuf, portBuf, stateBuf]);
    const handshakePacket = createPacket(0x00, handshakePayload);
    client.write(handshakePacket);

    // 2. Status Request packet (0x00, empty payload)
    const requestPacket = createPacket(0x00);
    client.write(requestPacket);
});

let rxBuf = Buffer.alloc(0);

client.on('data', (chunk) => {
    rxBuf = Buffer.concat([rxBuf, chunk]);

    const lenRes = readVarInt(rxBuf, 0);
    if (!lenRes) return;
    const totalPacketLen = lenRes.value;
    const headerLen = lenRes.bytesRead;

    if (rxBuf.length < headerLen + totalPacketLen) return; // wait for more data

    const packetPayload = rxBuf.subarray(headerLen, headerLen + totalPacketLen);
    const idRes = readVarInt(packetPayload, 0);
    const packetId = idRes.value;
    const body = packetPayload.subarray(idRes.bytesRead);

    if (packetId === 0x00) {
        // Status Response: String (VarInt length + utf8 string)
        const strLenRes = readVarInt(body, 0);
        const jsonStr = body.subarray(strLenRes.bytesRead, strLenRes.bytesRead + strLenRes.value).toString('utf8');
        console.log('[Ping] Received Status JSON length:', jsonStr.length);
        try {
            const statusObj = JSON.parse(jsonStr);
            console.log('[Ping] Server Version:', JSON.stringify(statusObj.version));
            console.log('[Ping] Players:', JSON.stringify(statusObj.players));
            console.log('[Ping] Description:', JSON.stringify(statusObj.description));
            if (statusObj.favicon) {
                console.log('[Ping] SUCCESS: Favicon present! Length:', statusObj.favicon.length);
                console.log('[Ping] Favicon prefix:', statusObj.favicon.substring(0, 32) + '...');
            } else {
                console.log('[Ping] FAIL: Favicon missing!');
                process.exit(1);
            }
        } catch (e) {
            console.error('[Ping] Failed to parse JSON:', e.message);
            console.error('Raw JSON:', jsonStr.substring(0, 200));
            process.exit(1);
        }
        client.end();
        process.exit(0);
    }
});

client.on('error', (err) => {
    console.error('[Ping] Connection error:', err.message);
    process.exit(1);
});
