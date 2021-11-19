import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// Instructions and Types: ////////////
//
//    add(R) sub(R)  addi(I) subi(I)
//    xor(R) xori(I) and(R)  jalr(I)
//    beq(B) bge(B)  blt(B)  jal(J)
//    lw(I)  sw(S)   lb(I)   sb(S)
//    srl(R) sra(R)  slti(I) srai(I?)
//
///////////////////////////////////////

public class Benzetim {

    private static int totalCycle = 0;
    private static int totalProcessedInstruction = 0;
    private static float totalExecutionTime = 0;
    private static int frequency = 0;
    private static int RTypeIPC = 0;
    private static int ITypeIPC = 0;
    private static int BTypeIPC = 0;
    private static int STypeIPC = 0;
    private static int JTypeIPC = 0;
    private static int PC = 0;
    private static int[] registers = new int[32];       // 32 register
    private static byte[] memory = new byte[1048576];   // 1 MB
    private static Dictionary<String, Integer> OPCodeMap;

//    public enum eInstructionType
//    {
//        R, I, S, B, J, INVALID
//    }

    public static void initInternals() {
        totalCycle = 0;
        totalProcessedInstruction = 0;
        totalExecutionTime = 0;
        frequency = 0;
        RTypeIPC = 0;
        ITypeIPC = 0;
        BTypeIPC = 0;
        STypeIPC = 0;
        JTypeIPC = 0;
        PC = 0;
        Arrays.fill(registers, 0);
        Arrays.fill(memory, (byte) 0);

        OPCodeMap = new Hashtable<String, Integer>();
        OPCodeMap.put("add"  ,0);
        OPCodeMap.put("sub"  ,1);
        OPCodeMap.put("addi" ,2);
        OPCodeMap.put("subi" ,3);
        OPCodeMap.put("xor"  ,4);
        OPCodeMap.put("xori" ,5);
        OPCodeMap.put("and"  ,6);
        OPCodeMap.put("jalr" ,7);
        OPCodeMap.put("beq"  ,8);
        OPCodeMap.put("bge"  ,9);
        OPCodeMap.put("blt"  ,10);
        OPCodeMap.put("jal"  ,11);
        OPCodeMap.put("lw"   ,12);
        OPCodeMap.put("sw"   ,13);
        OPCodeMap.put("lb"   ,14);
        OPCodeMap.put("sb"   ,15);
        OPCodeMap.put("srl"  ,16);
        OPCodeMap.put("sra"  ,17);
        OPCodeMap.put("slti" ,18);
        OPCodeMap.put("srai" ,19);
    }

    public static void readConfig(String configFileName) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(configFileName));
            String line = reader.readLine();

            while (line != null) {
                String[] data = line.split(" ");
                int value = Integer.parseInt(data[1]);

                switch (data[0])
                {
                    case "Frekans":
                        frequency = value;
                        break;
                    case "R":
                        RTypeIPC = value;
                        break;
                    case "I":
                        ITypeIPC = value;
                        break;
                    case "B":
                        BTypeIPC = value;
                        break;
                    case "S":
                        STypeIPC = value;
                        break;
                    case "J":
                        JTypeIPC = value;
                        break;
                    default:
                        System.out.printf("Invalid line data %s\n", line);
                        break;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readProgram(String programFileName) {
        BufferedReader reader;
        int opcode;
        int rd;
        int rs1;
        int rs2;
        int imm;
        int instruction;

        try {
            reader = new BufferedReader(new FileReader(programFileName));
            String line = reader.readLine();

            while (line != null) {
                String[] data = line.replaceAll("//", "").split(" ");

                if (!data[0].equals("")) {
                    int address = Integer.decode(data[0]);  // TODO: dont sure about this
                    String instructionName = data[1];

                    instruction = 0;
                    switch (instructionName) {
                        case "add":
                        case "sub":
                        case "xor":
                        case "and":
                        case "slr":
                        case "sla":
                            opcode = OPCodeMap.get(instructionName);
                            rd = Integer.parseInt(data[2].replace("x", ""));
                            rs1 = Integer.parseInt(data[3].replace("x", ""));
                            rs2 = Integer.parseInt(data[4].replace("x", ""));

                            // convert program line to instruction
                            instruction |= (0x7F & opcode);
                            instruction |= ((0x1F & rd) << 7);
                            instruction |= ((0x1F & rs1) << 15);
                            instruction |= ((0x1F & rs2) << 20);
                            break;
                        case "addi":
                        case "subi":
                        case "xori":
                        case "jalr":
                        case "lw":
                        case "lb":
                        case "slti":
                        case "srai":
                            opcode = OPCodeMap.get(instructionName);
                            rd = Integer.parseInt(data[2].replace("x", ""));
                            rs1 = Integer.parseInt(data[3].replace("x", ""));
                            imm = Integer.parseInt(data[4], 16);

                            instruction |= (0x7F & opcode);
                            instruction |= ((0x1F & rd) << 7);
                            instruction |= ((0x1F & rs1) << 15);
                            instruction |= ((0xFFF & imm) << 20);
                            break;
                        case "beq": // B and S types are handling the same way
                        case "bge":
                        case "blt":
                        case "sw":
                        case "sb":
                            opcode = OPCodeMap.get(instructionName);
                            rs1 = Integer.parseInt(data[2].replace("x", ""));
                            rs2 = Integer.parseInt(data[3].replace("x", ""));
                            imm = Integer.parseInt(data[4], 16);

                            instruction |= (0x7F & opcode);
                            instruction |= ((0x1F & imm) << 7);         // first part of imm
                            instruction |= ((0x1F & rs1) << 15);
                            instruction |= ((0x1F & rs2) << 20);

                            int immPart2 = ((0xFE0 & imm) >> 5);
                            instruction |= ((0x7F & immPart2) << 25);   // second part of imm
                            break;
                        case "jal":
                            opcode = OPCodeMap.get(instructionName);
                            rd = Integer.parseInt(data[2].replace("x", ""));
                            imm = Integer.parseInt(data[3], 16);

                            instruction |= (0x7F & opcode);
                            instruction |= ((0x1F & rd) << 7);
                            instruction |= ((0xFFFFF & imm) << 12);
                            break;
                        case "SON":
                            instruction = 0xFFFFFFFF;
                            break;
                        default:
                            System.out.printf("Invalid instruction: %s in line: %s\n", instructionName, line);
                            break;
                    }

                    // store instruction in memory
                    memory[address] = (byte) ((instruction & 0xFF000000) >> 24);
                    memory[address + 1] = (byte) ((instruction & 0x00FF0000) >> 16);
                    memory[address + 2] = (byte) ((instruction & 0x0000FF00) >> 8);
                    memory[address + 3] = (byte) ((instruction & 0x000000FF) >> 0);
                }

                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeRegisterValues() {
        File outputFile = new File("cikti.txt");
        FileWriter fWriter = null;

        try {
            fWriter = new FileWriter(outputFile);

            for (int regIndex = 0; regIndex < 32; regIndex++)
            {
                fWriter.write("Register[" + regIndex + "]: " + registers[regIndex] + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            //close resources
            try {
                fWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int fetchInstruction() {
        return ((memory[PC] & 0xff) << 24) + ((memory[PC+1] & 0xff) << 16) + ((memory[PC+2] & 0xff) << 8) + (memory[PC+3] & 0xff);
    }

    public static int getOPCode(int instruction) {
        return instruction & 0x7F;
    }

    public static int getRDValue(int instruction) {
        return ((0x1f << 7 ) & instruction) >> 7;
    }

    public static int getRS1Value(int instruction) {
        return ((0x1f << 15 ) & instruction) >> 15;
    }

    public static int getRS2Value(int instruction) {
        return ((0x1f << 20 ) & instruction) >> 20;
    }

    public static int getITypeImmValue(int instruction) {
        return (0xFFF00000 & instruction) >> 20;
    }

    public static int getBSTypeImmValue(int instruction) {
        int immPart1 = (0xF80 & instruction) << 13;
        int immPart2 = ((0xFE000000 & instruction) + immPart1) >> 20;

        return immPart2 & 0xFFF;
    }

    public static int getJTypeImmValue(int instruction) {
        return (0xFFFFF000 & instruction) >> 12;
    }

    public static int getInstructionType(int instruction) {
        int opcode = getOPCode(instruction);
        int instructionType = -1;

        // R: 0, I: 1, S:2, B:3, J:4
        switch (opcode) {
            case 0:
            case 1:
            case 4:
            case 6:
            case 16:
            case 17:
                instructionType = 0;
                break;
            case 2:
            case 3:
            case 5:
            case 7:
            case 12:
            case 14:
            case 18:
                instructionType = 1;
                break;
            case 13:
            case 15:
                instructionType = 2;
                break;
            case 8:
            case 9:
            case 10:
                instructionType = 3;
                break;
            case 11:
                instructionType = 4;
                break;
            default:
                instructionType = -1;
                break;
        }

        return instructionType;
    }

    public static int getInstructionIPC(int instructionType) {
        int IPC = 0;

        // R: 0, I: 1, S:2, B:3, J:4
        switch (instructionType) {
            case 0:
                IPC = RTypeIPC;
                break;
            case 1:
                IPC = ITypeIPC;
                break;
            case 2:
                IPC = STypeIPC;
                break;
            case 3:
                IPC = BTypeIPC;
                break;
            case 4:
                IPC = JTypeIPC;
                break;
            default:
                IPC = 0;
                break;
        }

        return IPC;
    }

    /////

    public static void instAdd(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] + registers[rs2];
        PC += 4;
    }

    public static void instSub(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] - registers[rs2];
        PC += 4;
    }

    public static void instAddi(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        registers[rd] = registers[rs1] + imm;
        PC += 4;
    }

    public static void instSubi(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        registers[rd] = registers[rs1] - imm;
        PC += 4;
    }

    public static void instXor(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] ^ registers[rs2];
        PC += 4;
    }

    public static void instXori(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        registers[rd] = registers[rs1] ^ imm;
        PC += 4;
    }

    public static void instAnd(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] & registers[rs2];
        PC += 4;
    }

    public static void instJalr(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        registers[rd] = PC + 4;    // save return address
        PC = registers[rs1] + imm;
    }

    public static void instBeq(int instruction) {
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);
        int imm = getBSTypeImmValue(instruction);

        if (registers[rs1] == registers[rs2])
        {
            PC += imm * 2;
        }
        else
        {
            PC += 4;
        }
    }

    public static void instBge(int instruction) {
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);
        int imm = getBSTypeImmValue(instruction);

        if (registers[rs1] >= registers[rs2])
        {
            PC += imm * 2;
        }
        else
        {
            PC += 4;
        }
    }

    public static void instBlt(int instruction) {
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);
        int imm = getBSTypeImmValue(instruction);

        if (registers[rs1] < registers[rs2])
        {
            PC += imm * 2;
        }
        else
        {
            PC += 4;
        }
    }

    public static void instJal(int instruction) {
        int rd = getRDValue(instruction);
        int imm = getJTypeImmValue(instruction);

        registers[rd] = PC + 4;    // save return address
        PC += (imm * 2);
    }

    public static void instLw(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        int addr = imm + registers[rs1];
        registers[rd] = ((memory[addr] & 0xff) << 24) + ((memory[addr+1] & 0xff) << 16) + ((memory[addr+2] & 0xff) << 8) + (memory[addr+3] & 0xff);
        PC += 4;
    }

    public static void instSw(int instruction) {
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);
        int imm = getBSTypeImmValue(instruction);

        int addr = imm + registers[rs1];
        memory[addr] = (byte) ((registers[rs2] & 0xFF000000) >> 24);
        memory[addr + 1] = (byte) ((registers[rs2] & 0x00FF0000) >> 16);
        memory[addr + 2] = (byte) ((registers[rs2] & 0x0000FF00) >> 8);
        memory[addr + 3] = (byte) ((registers[rs2] & 0x000000FF) >> 0);
        PC += 4;
    }

    public static void instLb(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        int addr = imm + registers[rs1];
        registers[rd] = memory[addr];
        PC += 4;
    }

    public static void instSb(int instruction) {
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);
        int imm = getBSTypeImmValue(instruction);

        int addr = imm + registers[rs1];
        memory[addr] = (byte) registers[rs2];
        PC += 4;
    }

    public static void instSrl(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] >>> registers[rs2];  // logical right shift
        PC += 4;
    }

    public static void instSra(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int rs2 = getRS2Value(instruction);

        registers[rd] = registers[rs1] >> registers[rs2];  // logical arithmetic shift
        PC += 4;
    }

    public static void instSlti(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int imm = getITypeImmValue(instruction);

        if (rs1 < imm)
        {
            registers[rd] = 1;
        }
        else
        {
            registers[rd] = 0;
        }
        PC += 4;
    }

    public static void instSrai(int instruction) {
        int rd = getRDValue(instruction);
        int rs1 = getRS1Value(instruction);
        int shamt = getITypeImmValue(instruction);

        registers[rd] = registers[rs1] >> shamt;
        PC += 4;
    }

    /////

    public static void simulateProgram() {
        boolean bContinue = true;
        int instruction = 0;
        int opcode = 0;
        int instructionType = 0;

        while (bContinue)
        {
            instruction = fetchInstruction();   // read instruction from memory
            opcode = getOPCode(instruction);    // get opcode of the instruction
            instructionType = getInstructionType(instruction); // R: 0, I: 1, S:2, B:3, J:4, INVALID: -1

            totalProcessedInstruction += 1;
            totalCycle = totalCycle + getInstructionIPC(instructionType);

            int a = PC;
            switch (opcode)
            {
                case 0:
                    instAdd(instruction);
                    break;
                case 1:
                    instSub(instruction);
                    break;
                case 2:
                    instAddi(instruction);
                    break;
                case 3:
                    instSubi(instruction);
                    break;
                case 4:
                    instXor(instruction);
                    break;
                case 5:
                    instXori(instruction);
                    break;
                case 6:
                    instAnd(instruction);
                    break;
                case 7:
                    instJalr(instruction);
                    break;
                case 8:
                    instBeq(instruction);
                    break;
                case 9:
                    instBge(instruction);
                    break;
                case 10:
                    instBlt(instruction);
                    break;
                case 11:
                    instJal(instruction);
                    break;
                case 12:
                    instLw(instruction);
                    break;
                case 13:
                    instSw(instruction);
                    break;
                case 14:
                    instLb(instruction);
                    break;
                case 15:
                    instSb(instruction);
                    break;
                case 16:
                    instSrl(instruction);
                    break;
                case 17:
                    instSra(instruction);
                    break;
                case 18:
                    instSlti(instruction);
                    break;
                case 19:
                    instSrai(instruction);
                    break;

                case 127:   // SON
                    bContinue = false;
                    break;
                default:
                    break;
            }
        }

        // 1MHz = 1000000 cycle/second
        totalExecutionTime = totalCycle * ((float)1 / (float)(frequency * 1000000));    // in seconds
    }

    public static void main(String[] args) {
        initInternals();

        if (args.length == 2) {
            String programFile = args[0];
            String configFile = args[1];

            readConfig(configFile);
            readProgram(programFile);
            simulateProgram();
            writeRegisterValues();

            System.out.println("Toplam Cevrim Sayisi: " + totalCycle);
            System.out.println("Yurutulen Toplam Buyruk Sayisi: " + totalProcessedInstruction);
            System.out.println("Toplam Yurutme Zamani: " + totalExecutionTime + " saniye");
        }
        else if (args.length == 3) {
            int totalCycle1;
            int totalCycle2;
            int totalProcessedInstruction1;
            int totalProcessedInstruction2;
            float totalExecutionTime1;
            float totalExecutionTime2;

            String programFile = args[0];
            String configFile1 = args[1];
            String configFile2 = args[2];

            readConfig(configFile1);
            readProgram(programFile);
            simulateProgram();

            totalCycle1 = totalCycle;
            totalProcessedInstruction1 = totalProcessedInstruction;
            totalExecutionTime1 = totalExecutionTime;

            readConfig(configFile2);
            readProgram(programFile);
            simulateProgram();

            totalCycle2 = totalCycle;
            totalProcessedInstruction2 = totalProcessedInstruction;
            totalExecutionTime2 = totalExecutionTime;

            if (totalExecutionTime1 < totalExecutionTime2)
            {
                System.out.println("Islemci1’in basarimi Islemci2’nin basarimindan " + totalExecutionTime2 / totalExecutionTime1 + " kat daha yuksek.");
            }
            else
            {
                System.out.println("Islemci2’nin basarimi Islemci1’in basarimindan " + totalExecutionTime1 / totalExecutionTime2 + " kat daha yuksek.");
            }
        }
        else {
            System.out.println("Incorrect usage. [java Benzetim program.txt islemci1-config.txt <islemci2-config.txt>]");
        }
    }
}
