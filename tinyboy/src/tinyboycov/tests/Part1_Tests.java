package tinyboycov.tests;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javr.core.AvrInstruction;
import javr.io.HexFile;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Part1_Tests {
	private static int PINB = 0x16;
	private static int BUTTON_UP = 1;
	private static int BUTTON_DOWN = 3;
	private static int BUTTON_LEFT = 4;
	private static int BUTTON_RIGHT = 5;

	@Test
	public void test_01() throws Exception {
		AvrInstruction[] instructions = new AvrInstruction[] {
				// Loop until button pressed
				new AvrInstruction.SBIS(PINB, BUTTON_UP),
				new AvrInstruction.RJMP(-2),
				new AvrInstruction.RJMP(-1),
		};
		TestUtils.checkCoverage(100.0,instructions);
	}

	@Test
	public void test_02() throws Exception {
		AvrInstruction[] instructions = new AvrInstruction[] {
				// Loop until button pressed
				new AvrInstruction.SBIS(PINB, BUTTON_DOWN),
				new AvrInstruction.RJMP(-2),
				new AvrInstruction.RJMP(-1),
		};
		TestUtils.checkCoverage(100.0,instructions);
	}

	@Test
	public void test_03() throws Exception {
		AvrInstruction[] instructions = new AvrInstruction[] {
				// Loop until button pressed
				new AvrInstruction.SBIS(PINB, BUTTON_LEFT),
				new AvrInstruction.RJMP(-2),
				new AvrInstruction.RJMP(-1),
		};
		TestUtils.checkCoverage(100.0,instructions);
	}

	@Test
	public void test_04() throws Exception {
		AvrInstruction[] instructions = new AvrInstruction[] {
				// Loop until button pressed
				new AvrInstruction.SBIS(PINB, BUTTON_RIGHT),
				new AvrInstruction.RJMP(-2),
				new AvrInstruction.RJMP(-1),
		};
		TestUtils.checkCoverage(100.0,instructions);
	}
}
