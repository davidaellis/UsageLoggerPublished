package geyerk.sensorlab.suselogger;

final class Ascii {

    //problematic characters have not been included
    private static final char
            thirtyThree = '!',

            thirtyFive = '#',
            thirtySix = '$',
            thirtySeven = '%',
            thirtyEight = '&',

            forty = '(',
            fortyOne = ')',
            fortyTwo = '*',
            fortyThree = '+',
            fortyFour = ',',
            fortyFive = '-',
            fortySix = '.',
            fortySeven = '/',
            fortyEight = '0',
            fortyNine = '1',
            fifty  = '2',
            fiftyOne = '3',
            fiftyTwo = '4',
            fiftyThree = '5',
            fiftyFour = '6',
            fiftyFive = '7',
            fiftySix = '8',
            fiftySeven = '9',
            fiftyEight = ':',
            fiftyNine = ';',
            sixty  = '<',
            sixtyOne = '=',
            sixtyTwo = '>',
            sixtyThree = '?',
            sixtyFour = '@',
            sixtyFive = 'A',
            sixtySix = 'B',
            sixtySeven = 'C',
            sixtyEight = 'D',
            sixtyNine = 'E',
            seventy  = 'F',
            seventyOne = 'G',
            seventyTwo = 'H',
            seventyThree = 'I',
            seventyFour = 'J',
            seventyFive = 'K',
            seventySix = 'L',
            seventySeven = 'M',
            seventyEight = 'N',
            seventyNine = 'O',
            eighty  = 'P',
            eightyOne = 'Q',
            eightyTwo = 'R',
            eightyThree = 'S',
            eightyFour = 'T',
            eightyFive = 'U',
            eightySix = 'V',
            eightySeven = 'W',
            eightyEight = 'X',
            eightyNine = 'Y',
            ninety  = 'Z',
            ninetyOne = '[',

            ninetyThree = ']',
            ninetyFour = '^',
            ninetyFive = '_',

            ninetySeven = 'a',
            ninetyEight = 'b',
            ninetyNine = 'c',
            oneHundred  = 'd',
            oneHundredOne = 'e',
            oneHundredTwo = 'f',
            oneHundredThree = 'g',
            oneHundredFour = 'h',
            oneHundredFive = 'i',
            oneHundredSix = 'j',
            oneHundredSeven = 'k',
            oneHundredEight = 'l',
            oneHundredNine = 'm',
            oneHundredAnd  = 'n',
            oneHundredAndEleven = 'o',
            oneHundredAndTwelve = 'p',
            oneHundredAndThirteen = 'q',
            oneHundredAndFourteen = 'r',
            oneHundredAndFifteen = 's',
            oneHundredAndSixteen = 't',
            oneHundredAndSeventeen = 'u',
            oneHundredAndEighteen = 'v',
            oneHundredAndNineteen = 'w',
            oneHundredAndTwenty  = 'x',
            oneHundredAndTwentyOne = 'y',
            oneHundredAndTwentyTwo = 'z',
            oneHundredAndTwentyThree = '{',
            oneHundredAndTwentyFour = '|',
            oneHundredAndTwentyFive = '}',
            oneHundredAndTwentySix = '~';


    char returnAscii(int requestedChar){
        if(requestedChar > 32 && requestedChar < 127){
            if(isNotProblematicChar(requestedChar)){
                switch (requestedChar){
                    case 33:return thirtyThree;
                    case 35:return thirtyFive;
                    case 36:return thirtySix;
                    case 37:return thirtySeven;
                    case 38:return thirtyEight;
                    case 40:return forty;
                    case 41:return fortyOne;
                    case 42:return fortyTwo;
                    case 43:return fortyThree;
                    case 44:return fortyFour;
                    case 45:return fortyFive;
                    case 46:return fortySix;
                    case 47:return fortySeven;
                    case 48:return fortyEight;
                    case 49:return fortyNine;
                    case 50:return fifty ;
                    case 51:return fiftyOne;
                    case 52:return fiftyTwo;
                    case 53:return fiftyThree;
                    case 54:return fiftyFour;
                    case 55:return fiftyFive;
                    case 56:return fiftySix;
                    case 57:return fiftySeven;
                    case 58:return fiftyEight;
                    case 59:return fiftyNine;
                    case 60:return sixty ;
                    case 61:return sixtyOne;
                    case 62:return sixtyTwo;
                    case 63:return sixtyThree;
                    case 64:return sixtyFour;
                    case 65:return sixtyFive;
                    case 66:return sixtySix;
                    case 67:return sixtySeven;
                    case 68:return sixtyEight;
                    case 69:return sixtyNine;
                    case 70:return seventy ;
                    case 71:return seventyOne;
                    case 72:return seventyTwo;
                    case 73:return seventyThree;
                    case 74:return seventyFour;
                    case 75:return seventyFive;
                    case 76:return seventySix;
                    case 77:return seventySeven;
                    case 78:return seventyEight;
                    case 79:return seventyNine;
                    case 80:return eighty ;
                    case 81:return eightyOne;
                    case 82:return eightyTwo;
                    case 83:return eightyThree;
                    case 84:return eightyFour;
                    case 85:return eightyFive;
                    case 86:return eightySix;
                    case 87:return eightySeven;
                    case 88:return eightyEight;
                    case 89:return eightyNine;
                    case 90:return ninety ;
                    case 91:return ninetyOne;
                    case 93:return ninetyThree;
                    case 94:return ninetyFour;
                    case 95:return ninetyFive;
                    case 97:return ninetySeven;
                    case 98:return ninetyEight;
                    case 99:return ninetyNine;
                    case 100:return oneHundred ;
                    case 101:return oneHundredOne;
                    case 102:return oneHundredTwo;
                    case 103:return oneHundredThree;
                    case 104:return oneHundredFour;
                    case 105:return oneHundredFive;
                    case 106:return oneHundredSix;
                    case 107:return oneHundredSeven;
                    case 108:return oneHundredEight;
                    case 109:return oneHundredNine;
                    case 110:return oneHundredAnd ;
                    case 111:return oneHundredAndEleven;
                    case 112:return oneHundredAndTwelve;
                    case 113:return oneHundredAndThirteen;
                    case 114:return oneHundredAndFourteen;
                    case 115:return oneHundredAndFifteen;
                    case 116:return oneHundredAndSixteen;
                    case 117:return oneHundredAndSeventeen;
                    case 118:return oneHundredAndEighteen;
                    case 119:return oneHundredAndNineteen;
                    case 120:return oneHundredAndTwenty ;
                    case 121:return oneHundredAndTwentyOne;
                    case 122:return oneHundredAndTwentyTwo;
                    case 123:return oneHundredAndTwentyThree;
                    case 124:return oneHundredAndTwentyFour;
                    case 125:return oneHundredAndTwentyFive;
                    case 126:return oneHundredAndTwentySix;
                }
            }else{
                return ' ';
            }
        }
        return ' ';
    }

    private boolean isNotProblematicChar(int requestedChar) {
        switch(requestedChar){
            case 34:
            case 39:
            case 92:
            case 96:
                return false;
            default:
                return true;
        }
    }
}
