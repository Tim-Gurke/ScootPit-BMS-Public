package de.wortmonster.jbdtrigger;

/** JBD basic-info reply. Only complete, checksum-valid frames become measurements. */
final class BmsPacket {
    final double voltage, current, remainingAh, fullAh;
    final int soc;
    final boolean chargeEnabled, dischargeEnabled;
    final double[] temperatures;
    private BmsPacket(byte[] b) {
        voltage = u16(b, 4) / 100.0;
        current = (short) u16(b, 6) / 100.0;
        remainingAh = u16(b, 8) / 100.0;
        fullAh = u16(b, 10) / 100.0;
        chargeEnabled = (b[24] & 1) != 0;
        dischargeEnabled = (b[24] & 2) != 0;
        soc = (b[23] & 255) <= 100 ? b[23] & 255 : -1;
        int count = b[26] & 255;
        temperatures = new double[count];
        for (int i = 0; i < count; i++) temperatures[i] = (u16(b, 27 + i * 2) - 2731) / 10.0;
    }
    static BmsPacket decode(byte[] b) {
        if (b == null || b.length < 30 || (b[0] & 255) != 0xdd
                || (b[1] & 255) != 3 || b[2] != 0) return null;
        int length = b[3] & 255;
        if (length < 23 || b.length != length + 7 || (b[b.length - 1] & 255) != 0x77) return null;
        int sum = 0;
        for (int i = 2; i < length + 4; i++) sum += b[i] & 255;
        if (((-sum) & 65535) != u16(b, length + 4)) return null;
        if (23 + (b[26] & 255) * 2 > length) return null;
        return new BmsPacket(b);
    }
    private static int u16(byte[] b, int i) { return ((b[i] & 255) << 8) | (b[i + 1] & 255); }
}
