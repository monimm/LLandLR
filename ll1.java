package com.moni;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Demo {
    public static void main(String[] args) {
        Test test = new Test();
        test.getNvNt();
        test.Init();
        test.createTable();
        test.analyzeLL();
        //test.analyzeSLR();
        test.ouput();
    }
}

class Test {
    //单个符号first集
    public HashMap<Character, HashSet<Character>> firstSet = new HashMap<>();
    //符号串first集
    public HashMap<String, HashSet<Character>> firstSetX = new HashMap<>();
    //开始符
    public static char S = 'S';
    public HashMap<Character, HashSet<Character>> followSet = new HashMap<>();
    //非终结符
    public HashSet<Character> VnSet = new HashSet<>();
    //终结符
    public HashSet<Character> VtSet = new HashSet<>();
    //非终结符-产生式集合
    public HashMap<Character, ArrayList<String>> experssionSet = new HashMap<>();
    // E: TK | K: +TK $ | T : FM | M: *FM $|F :i (E)
    public String[][] table;
    public String[][] tableSLR = {
            {"", "i", "+", "*", "(", ")", "$", "E", "T", "F"},
            {"0", "s5", "", "", "s4", "", "", "1", "2", "3"},
            {"1", "", "s6", "", "", "", "acc", "", "", ""},
            {"2", "", "r2", "s7", "", "r2", "r2", "", "", ""},
            {"3", "", "r4", "r4", "", "r4", "r4", "", "", ""},
            {"4", "s5", "", "", "s4", "", "", "8", "2", "3"},
            {"5", "", "r6", "r6", "", "r6", "r6", "", "", ""},
            {"6", "s5", "", "", "s4", "", "", "", "9", "3"},
            {"7", "s5", "", "", "s4", "", "", "", "", "10"},
            {"8", "", "s6", "", "", "s11", "", "", "", ""},
            {"9", "", "r1", "s7", "", "r1", "r1", "", "", ""},
            {"10", "", "r3", "r3", "", "r3", "r3", "", "", ""},
            {"11", "", "r5", "r5", "", "r5", "r5", "", "", ""}};

    public String[] inputExperssion = { "S->I", "S->o", "I->i(E)SL", "L->eS", "L->~", "E->a", "E->b"};
    public Stack<Character> analyzeStatck = new Stack<>();
    public Stack<String> stackState = new Stack<>();
    public Stack<Character> stackSymbol = new Stack<>();
    public String strInput = "i(a)i(b)oeo$";
    public String action = "";
    public String[] LRGS = {"E->E+T", "E->T", "T->T*F", "T->F", "F->(E)", "F->i"};
    int index = 0;

    public void Init() {
        //获取生成式
        for (String e : inputExperssion) {
            String[] str = e.split("->");
            char c = str[0].charAt(0);
            ArrayList<String> list = experssionSet.containsKey(c) ? experssionSet.get(c) : new ArrayList<>();
            list.add(str[1]);
            experssionSet.put(c, list);
        }
        //构造非终结符的first集
        for (char c : VnSet)
            getFirst(c);
        //构造开始符的follow集
        getFollow(S);
        //构造非终结符的follow集
        for (char c : VnSet)
            getFollow(c);
    }

    /**
     * 先求非终结符，再求终结符
     */
    public void getNvNt() {
        for (String e : inputExperssion)
            VnSet.add(e.split("->")[0].charAt(0));
        for (String e : inputExperssion)
            for (char c : e.split("->")[1].toCharArray())
                if (!VnSet.contains(c))
                    VtSet.add(c);
    }

    public void getFirst(char c) {
        if (firstSet.containsKey(c))
            return;
        HashSet<Character> set = new HashSet<>();
        // 若c为终结符 直接添加
        if (VtSet.contains(c)) {
            set.add(c);
            firstSet.put(c, set);
            return;
        }
        // c为非终结符 处理其每条产生式
        for (String s : experssionSet.get(c)) {
            if ("~".equals(c)) {
                set.add('~');
            } else {
                for (char cur : s.toCharArray()) {
                    if (!firstSet.containsKey(cur))
                        getFirst(cur);
                    HashSet<Character> curFirst = firstSet.get(cur);
                    set.addAll(curFirst);
                    if (!curFirst.contains('~'))
                        break;
                }
            }
        }
        firstSet.put(c, set);
    }

    public void getFirst(String s) {
        if (firstSetX.containsKey(s))
            return;
        HashSet<Character> set = new HashSet<>();
        // 从左往右扫描该式
        int i = 0;
        while (i < s.length()) {
            char cur = s.charAt(i);
            if (!firstSet.containsKey(cur))
                getFirst(cur);
            HashSet<Character> rightSet = firstSet.get(cur);
            // 将其非空 first集加入左部
            set.addAll(rightSet);
            // 若包含空串 处理下一个符号
            if (rightSet.contains('~'))
                i++;
            else
                break;
            // 若到了尾部 即所有符号的first集都包含空串 把空串加入fisrt集
            if (i == s.length()) {
                set.add('~');
            }
        }
        firstSetX.put(s, set);
    }


    public void getFollow(char c) {
        ArrayList<String> list = experssionSet.get(c);
        HashSet<Character> leftFollowSet = followSet.containsKey(c) ? followSet.get(c) : new HashSet<>();
        //如果是开始符 添加 $
        if (c == S)
            leftFollowSet.add('$');
        //查找输入的所有产生式，添加c的后跟 终结符
        for (char ch : VnSet)
            for (String s : experssionSet.get(ch))
                for (int i = 0; i < s.length(); i++)
                    if (c == s.charAt(i) && i + 1 < s.length() && VtSet.contains(s.charAt(i + 1)))
                        leftFollowSet.add(s.charAt(i + 1));
        followSet.put(c, leftFollowSet);
        //反向扫描处理c的每一条产生式
        for (String s : list) {
            int i = s.length() - 1;
            while (i >= 0) {
                char cur = s.charAt(i);
                //只处理非终结符  I->i(E)SL
                if (VnSet.contains(cur)) {
                    // 都按 A->αBβ  形式处理
                    //1.若β不存在   followA 加入 followB
                    //2.若β存在，把β的非空first集  加入followB
                    //3.若β存在  且first(β)包含空串  followA 加入 followB
                    String right = s.substring(i + 1);
                    HashSet<Character> rightFirstSet;
                    if(!followSet.containsKey(cur))
                        getFollow(cur);
                    HashSet<Character> curFollowSet = followSet.get(cur);
                    //先找出first(β),将非空的加入followB
                    if (0 == right.length()) {
                        curFollowSet.addAll(leftFollowSet);
                    } else {
                        if (1 == right.length()) {
                            if (!firstSet.containsKey(right.charAt(0)))
                                getFirst(right.charAt(0));
                            rightFirstSet = firstSet.get(right.charAt(0));
                        } else {
                            if (!firstSetX.containsKey(right))
                                getFirst(right);
                            rightFirstSet = firstSetX.get(right);
                        }
                        for (char var : rightFirstSet)
                            if (var != '~')
                                curFollowSet.add(var);
                        // 若first(β)包含空串,将followA加入followB
                        if (rightFirstSet.contains('~'))
                            curFollowSet.addAll(leftFollowSet);
                    }
                    followSet.put(cur, curFollowSet);
                }
                i--;
            }
        }
    }


    public void createTable() {
        Object[] VtArray = VtSet.toArray();
        Object[] VnArray = VnSet.toArray();
        // 预测分析表初始化
        table = new String[VnArray.length + 1][VtArray.length + 1];
        table[0][0] = "Vn/Vt";
        //初始化首行首列
        for (int i = 0; i < VtArray.length; i++)
            table[0][i + 1] = (VtArray[i].toString().charAt(0) == '~') ? "$" : VtArray[i].toString();
        for (int i = 0; i < VnArray.length; i++)
            table[i + 1][0] = VnArray[i] + "";
        //全部置error
        for (int i = 0; i < VnArray.length; i++)
            for (int j = 0; j < VtArray.length; j++)
                table[i + 1][j + 1] = "error";
        //插入生成式
        for (char A : VnSet) {
            for (String s : experssionSet.get(A)) {
                if (!firstSetX.containsKey(s))
                    getFirst(s);
                HashSet<Character> set = firstSetX.get(s);
                for (char a : set)
                    insert(A, a, s);
                if (set.contains('~')) {
                    HashSet<Character> setFollow = followSet.get(A);
                    if (setFollow.contains('$'))
                        insert(A, '$', s);
                    for (char b : setFollow)
                        insert(A, b, s);
                }
            }
        }
    }

    public void analyzeLL() {
        System.out.println("****************LL分析过程**********");
        System.out.println("               Stack           Input     Action");
        analyzeStatck.push('$');
        analyzeStatck.push(S);
        displayLL();
        char X = analyzeStatck.peek();
        while (X != '$') {
            char a = strInput.charAt(index);
            if (X == a) {
                action = "match " + analyzeStatck.peek();
                analyzeStatck.pop();
                index++;
            } else if (VtSet.contains(X))
                return;
            else if (find(X, a).equals("error"))
                return;
            else if (find(X, a).equals("~")) {
                analyzeStatck.pop();
                action = X + "->~";
            } else {
                String str = find(X, a);
                if (str != "") {
                    action = X + "->" + str;
                    analyzeStatck.pop();
                    int len = str.length();
                    for (int i = len - 1; i >= 0; i--)
                        analyzeStatck.push(str.charAt(i));
                } else {
                    System.out.println("error at '" + strInput.charAt(index) + " in " + index);
                    return;
                }
            }
            X = analyzeStatck.peek();
            displayLL();
        }
        System.out.println("analyze LL1 successfully");
        System.out.println("****************LL分析过程**********");
    }

    public void analyzeSLR() {
        action = "";
        index = 0;
        stackState.push("0");
        char a = strInput.charAt(index);
        System.out.println("****************SLR分析过程**********");
        System.out.println("                    State         Symbol        Input         Action");
        this.displaySLR();
        while (true) {
            String s = stackState.peek();
            // 查表为移进
            if (Action(s, a).charAt(0) == 's') {
                stackState.push(Action(s, a).substring(1));
                stackSymbol.push(a);
                a = strInput.charAt(++index);
                action = "shift ";
                displaySLR();
            }
            // 查表为归约
            else if (Action(s, a).charAt(0) == 'r') {
                // 获取文法串
                String str = LRGS[Integer.parseInt(Action(s, a).substring(1)) - 1];
                int len = str.substring(3).length();
                // 弹出右部长度的符号和状态
                for (int i = 0; i < len; i++) {
                    stackSymbol.pop();
                    stackState.pop();
                }
                // goto的值进栈
                String t = stackState.peek();
                stackState.push(Action(t, str.charAt(0)));
                stackSymbol.push(str.charAt(0));
                action = "reduce:" + str;
                displaySLR();
            } else if (Action(s, a) == "acc")
                break;
            else
                return;
        }
        System.out.println("analyze SLR successfully");
        System.out.println("****************SLR分析过程**********");
    }

    public String Action(String s, char a) {
        for (int i = 1; i < 13; i++)
            if (tableSLR[i][0].equals(s))
                for (int j = 1; j < 10; j++)
                    if (tableSLR[0][j].charAt(0) == a)
                        return tableSLR[i][j];
        return "";
    }

    public String find(char X, char a) {
        for (int i = 0; i < VnSet.size() + 1; i++) {
            if (table[i][0].charAt(0) == X)
                for (int j = 0; j < VtSet.size() + 1; j++) {
                    if (table[0][j].charAt(0) == a)
                        return table[i][j];
                }
        }
        return "";
    }

    public void insert(char X, char a, String s) {
        if (a == '~') a = '$';
        for (int i = 0; i < VnSet.size() + 1; i++) {
            if (table[i][0].charAt(0) == X)
                for (int j = 0; j < VtSet.size() + 1; j++) {
                    if (table[0][j].charAt(0) == a) {
                        table[i][j] = s;
                        return;
                    }
                }
        }
    }

    public void displayLL() {
        // 输出 LL1
        Stack<Character> s = analyzeStatck;
        System.out.printf("%23s", s);
        System.out.printf("%13s", strInput.substring(index));
        System.out.printf("%10s", action);
        System.out.println();
    }

    public void displaySLR() {
        // 输出 SLR
        System.out.printf("%25s", stackState);
        System.out.printf("%15s", stackSymbol);
        System.out.printf("%15s", strInput.substring(index));
        System.out.printf("%15s", action);
        System.out.println();
    }

    public void ouput() {
        System.out.println("*********first集********");
        for (Character c : VnSet) {
            HashSet<Character> set = firstSet.get(c);
            System.out.printf("%10s", c + "  ->   ");
            for (Character var : set)
                System.out.print(var);
            System.out.println();
        }
        System.out.println("**********first集**********");
        System.out.println("*********firstX集********");
        Set<String> setStr = firstSetX.keySet();
        for (String s : setStr) {
            HashSet<Character> set = firstSetX.get(s);
            System.out.printf("%10s", s + "  ->   ");
            for (Character var : set)
                System.out.print(var);
            System.out.println();
        }
        System.out.println("**********firstX集**********");
        System.out.println("**********follow集*********");

        for (Character c : VnSet) {
            HashSet<Character> set = followSet.get(c);
            System.out.print("Follow " + c + ":");
            for (Character var : set)
                System.out.print(var);
            System.out.println();
        }
        System.out.println("**********follow集**********");

        System.out.println("**********LL1预测分析表********");

        for (int i = 0; i < VnSet.size() + 1; i++) {
            for (int j = 0; j < VtSet.size() + 1; j++) {
                System.out.printf("%6s", table[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("**********LL1预测分析表********");

        System.out.println("**********SLR语法分析表********");

        for (int i = 0; i < 12 + 1; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.printf("%6s", tableSLR[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("**********SLR语法分析表********");

    }

}

