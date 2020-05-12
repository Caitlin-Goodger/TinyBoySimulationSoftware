package tinyboycov.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javr.core.AVR;
import javr.core.AvrDecoder;
import javr.core.AvrInstruction;
import javr.io.HexFile;
import javr.memory.ByteMemory;
import tinyboy.util.AutomatedTester;
import tinyboy.util.CoverageAnalysis;
import tinyboycov.core.TinyBoyInputGenerator;
import tinyboycov.util.ProcessTimerMethod;

/**
 * Test utilities for the assignment
 *
 * *** DO NOT CHANGE THIS FILE ***
 *
 * @author David J. Pearce
 *
 */
public class TestUtils {
	/**
	 * Amount of time test is allowed to execute for. Note that this is an aid only,
	 * and should given a result indicative of the what the automated marking system
	 * will do.
	 */
	private static final long TIMEOUT = 300_000; // 5mins
	/**
	 * Specifies where to find the firmware images.
	 */
	private static final String FIRMWARE_DIR = "tests/".replace("/", File.separator);

	/**
	 * Perform automated coverage analysis of a given instruction sequence. This
	 * sequence is first converted into a firmware image before being uploaded onto
	 * the tiny boy.
	 *
	 * @param firmware The firmware image being used
	 * @return
	 * @throws Throwable
	 */
	public static void checkCoverage(double target, AvrInstruction... instructions) throws Exception {
		// Determine test name
		String name = getMethodName(1);
		// Generate firmware
		HexFile firmware = assemble(instructions);
		// Done
		TestUtils.checkCoverage(name, firmware, target, false, false, 1, 1);
	}

	/**
	 * Perform automated coverage analysis of a given firmware file. The firmware is
	 * first loaded from the appropriate location and then uploaded into the
	 * TinyBoy.
	 *
	 * @param filename   File name of firmware image
	 * @return
	 * @throws Throwable
	 */
	public static void checkCoverage(double target, String filename, boolean timeout, boolean gui, int nThreads, int batchSize) throws Exception {
		// Determine test name
		String name = getMethodName(1) + ":" + filename;
		// Generate firmware
		HexFile.Reader reader = new HexFile.Reader(new FileReader(FIRMWARE_DIR + filename));
		HexFile firmware = reader.readAll();
		// Done
		TestUtils.checkCoverage(name, firmware, target, timeout, gui, nThreads, batchSize);
	}

	/**
	 * Perform automated coverage analysis of a given firmware.
	 *
	 * @param name     Name used for printing out report
	 * @param firmware The firmware image being used
	 * @return
	 * @throws IOException
	 * @throws Throwable
	 */
	public static void checkCoverage(String name, HexFile firmware, double target, boolean timeout, boolean gui, int nThreads, int batchSize) throws Exception {
		if (timeout) {
			String testClassName = TestUtils.class.getName();
			try {
				ProcessTimerMethod.Outcome r = ProcessTimerMethod.exec(TIMEOUT, testClassName,
						"checkCoverageWithTimeout", name, firmware, target, gui, nThreads, batchSize);
				//
				System.out.println(new String(r.getStdout()));
				System.out.println(new String(r.getStderr()));
				//
				if (r.exitCode() == null) {
					fail("Timeout occurred");
				} else if (r.exitCode() != 0) {
					fail("Test failure");
				} else {

				}
			} catch (IOException e) {
				throw e;
			} catch (Throwable e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		} else {
			checkCoverageWithTimeout(name, firmware, target, gui, nThreads, batchSize);
		}
	}

	public static void checkCoverageWithTimeout(String name, HexFile firmware, Double target, Boolean gui, Integer nThreads, Integer batchSize) throws Exception {
		long time = System.currentTimeMillis();
		// Construct the input generator
		AutomatedTester.InputGenerator<?> generator = new TinyBoyInputGenerator();
		// Construct the fuzz tester
		AutomatedTester tester = new AutomatedTester(firmware, generator, gui, nThreads, batchSize);
		// Run the fuzz tester for 50 inputs.
		CoverageAnalysis coverage = tester.run(target);
		// Record time
		time = System.currentTimeMillis() - time;
		// Destroy GUI (if present)
		tester.destroy();
		// Check wether the target was reached.
		if (coverage.getBranchCoverage() < target) {
			// Indicates a fail
			System.out.println("===============================================");
			System.out.println(name + " (" + String.format("%.2f", coverage.getInstructionCoverage())
					+ "% instructions, " + String.format("%.2f", coverage.getBranchCoverage()) + "% branches, " + time + "ms)");
			System.out.println("===============================================");
			printDisassembly(firmware, coverage);
			fail("Branch coverage failed to meet target of " + target + "%");
		} else {
			printDisassembly(firmware, coverage);
			System.out.println("TIME: " + time + "ms");
		}

	}

	/**
	 * This is an "interesting" method which determines the name of a method on the
	 * call stack, as determined by a given index relative to the position of the
	 * caller.
	 *
	 * @param n
	 * @return
	 */
	private static String getMethodName(int n) {
		StackTraceElement[] trace = new Exception().getStackTrace();
		return trace[1 + n].getMethodName();
	}


	/**
	 * Responsible for turning a given sequence of instructions into a hexfile, so
	 * that it can in turn be uploaded to the tiny boy.
	 *
	 * @param instructions
	 * @return
	 */
	private static HexFile assemble(AvrInstruction... instructions) {
		byte[][] chunks = new byte[instructions.length][];
		int total = 0;
		// Encode each instruction into a byte sequence
		for(int i=0;i!=instructions.length;++i) {
			byte[] bytes = instructions[i].getBytes();
			chunks[i] = bytes;
			total = total + bytes.length;
		}
		// Flatten the chunks into a sequence
		byte[] sequence = new byte[total];
		//
		for(int i=0,j=0;i!=chunks.length;++i) {
			byte[] chunk = chunks[i];
			System.arraycopy(chunk, 0, sequence, j, chunk.length);
			j = j + chunk.length;
		}
		// Finally, create the hex file!
		return HexFile.toHexFile(sequence,16);
	}
	/**
	 * Disassemble the firmware image in order to provide useful feedback. This is
	 * essentially provided to help with debugging, and to determine where the
	 * unreachable code is in a given firmware image.
	 *
	 * @param tinyBoy
	 * @param The     set of reachable instructions. This is critical to determining
	 *                what is a valid statement, versus what is not.
	 * @return
	 */
	public static void printDisassembly(HexFile firmware, CoverageAnalysis coverage) {
		AvrDecoder decoder = new AvrDecoder();
		AVR.Memory code = new ByteMemory(8192);
		firmware.uploadTo(code);
		int size = code.size() / 2;
		boolean ignoring = false;
		int instructions = 0;
		int coveredInstructions = 0;
		int branches = 0;
		int coveredBranches = 0;
		for (int i = 0; i != size;) {
			if (coverage.isReachableInstruction(i)) {
				AvrInstruction insn = decoder.decode(code, i);
				System.out.print(String.format("%04X", i));
				instructions++;
				if (coverage.wasCovered(i)) {
					System.out.print(" [*] ");
					coveredInstructions++;
				} else {
					System.out.print(" [ ] ");
				}
				System.out.print(insn.toString());
				if (coverage.isConditionalBranchCovered(i)) {
					System.out.println("\t<<<<<<<<<<<<<<<<<<<< (" + branches++ + ")");
					coveredBranches++;
				} else if (coverage.isConditionalBranch(i)) {
					System.out.println("\t<<<<<<<<<<<<<<<<<<<< UNCOVERED (" + branches++ + ")");
				} else {
					System.out.println();
				}
				i = i + insn.getWidth();
				ignoring = false;
			} else {
				if (!ignoring) {
					System.out.println(" ... ");
					ignoring = true;
				}
				i = i + 1;
			}
		}
		System.out.println(
				"Instruction Coverage = " + coveredInstructions + " / " + instructions + " (" + code.size() + ")");
		System.out.println("Branch Coverage = " + coveredBranches + " / " + branches + " (" + coverage.getBranchCoverage() + "%)");
	}

}
