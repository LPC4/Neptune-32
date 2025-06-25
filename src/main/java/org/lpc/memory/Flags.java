package org.lpc.memory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Flags {
    private boolean zero;
    private boolean negative;
    private boolean carry;
    private boolean overflow;

    public void update(int result) {
        zero = result == 0;
        negative = result < 0;
    }

    public void clear() {
        zero = negative = carry = overflow = false;
    }

    public void updateAdd(int a, int b, int result) {
        update(result);

        long unsignedResult = (a & 0xFFFFFFFFL) + (b & 0xFFFFFFFFL);
        carry = (unsignedResult >>> 32) != 0;

        overflow = ((a ^ result) & (b ^ result) & 0x80000000) != 0;
    }

    public void updateSub(int a, int b, int result) {
        update(result);

        long unsignedResult = (a & 0xFFFFFFFFL) - (b & 0xFFFFFFFFL);
        carry = unsignedResult < 0;

        overflow = ((a ^ b) & (a ^ result) & 0x80000000) != 0;
    }
}
