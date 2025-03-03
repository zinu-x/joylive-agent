/*
 * AhoCorasickDoubleArrayTrie Project
 *      https://github.com/hankcs/AhoCorasickDoubleArrayTrie
 *
 * Copyright 2008-2016 hankcs <me@hankcs.com>
 * You may modify and redistribute as long as this attribution remains.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jd.live.agent.core.util.trie.hankcs;

import com.jd.live.agent.core.util.trie.Trie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * An implementation of Aho Corasick algorithm based on Double Array Trie
 *
 * @param <V> the type of value
 * @author hankcs
 */
public class AhoCorasickDoubleArrayTrie<V> implements Serializable, Trie<V> {
    /**
     * check array of the Double Array Trie structure
     */
    protected int[] check;
    /**
     * base array of the Double Array Trie structure
     */
    protected int[] base;
    /**
     * fail table of the Aho Corasick automata
     */
    protected int[] fail;
    /**
     * output table of the Aho Corasick automata
     */
    protected int[][] output;
    /**
     * outer value array
     */
    protected V[] v;

    /**
     * the length of every key
     */
    protected int[] l;

    /**
     * the size of base and check array
     */
    protected int size;

    /**
     * Parse text
     *
     * @param text The text
     * @return a list of outputs
     */
    public List<Hit<V>> parseText(CharSequence text) {
        int position = 1;
        int currentState = 0;
        List<Hit<V>> collectedEmits = new ArrayList<Hit<V>>();
        int length = text.length();
        for (int i = 0; i < length; ++i) {
            currentState = getState(currentState, text.charAt(i));
            storeEmits(position, currentState, collectedEmits);
            ++position;
        }

        return collectedEmits;
    }

    /**
     * Parse text
     *
     * @param text      The text
     * @param processor A processor which handles the output
     */
    public void parseText(CharSequence text, IHit<V> processor) {
        int position = 1;
        int currentState = 0;
        int length = text.length();
        for (int i = 0; i < length; ++i) {
            currentState = getState(currentState, text.charAt(i));
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                for (int hit : hitArray) {
                    processor.hit(position - l[hit], position, v[hit]);
                }
            }
            ++position;
        }
    }

    /**
     * Parse text
     *
     * @param text      The text
     * @param processor A processor which handles the output
     */
    public void parseText(CharSequence text, IHitCancellable<V> processor) {
        int currentState = 0;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            final int position = i + 1;
            currentState = getState(currentState, text.charAt(i));
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                for (int hit : hitArray) {
                    boolean proceed = processor.hit(position - l[hit], position, v[hit]);
                    if (!proceed) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Parse text
     *
     * @param text      The text
     * @param processor A processor which handles the output
     */
    public void parseText(char[] text, IHit<V> processor) {
        int position = 1;
        int currentState = 0;
        for (char c : text) {
            currentState = getState(currentState, c);
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                for (int hit : hitArray) {
                    processor.hit(position - l[hit], position, v[hit]);
                }
            }
            ++position;
        }
    }

    /**
     * Parse text
     *
     * @param text      The text
     * @param processor A processor which handles the output
     */
    public void parseText(char[] text, IHitFull<V> processor) {
        int position = 1;
        int currentState = 0;
        for (char c : text) {
            currentState = getState(currentState, c);
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                for (int hit : hitArray) {
                    processor.hit(position - l[hit], position, v[hit], hit);
                }
            }
            ++position;
        }
    }

    /**
     * Checks that string contains at least one substring
     *
     * @param text source text to check
     * @return {@code true} if string contains at least one substring
     */
    public boolean matches(CharSequence text) {
        int currentState = 0;
        int length = text.length();
        for (int i = 0; i < length; ++i) {
            currentState = getState(currentState, text.charAt(i));
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search first match in string
     *
     * @param text source text to check
     * @return first match or {@code null} if there are no matches
     */
    public Hit<V> findFirst(CharSequence text) {
        int position = 1;
        int currentState = 0;
        int length = text.length();
        for (int i = 0; i < length; ++i) {
            currentState = getState(currentState, text.charAt(i));
            int[] hitArray = output[currentState];
            if (hitArray != null) {
                int hitIndex = hitArray[0];
                return new Hit<V>(position - l[hitIndex], position, v[hitIndex]);
            }
            ++position;
        }
        return null;
    }


    /**
     * Save
     *
     * @param out An ObjectOutputStream object
     * @throws IOException Some IOException
     */
    public void save(ObjectOutputStream out) throws IOException {
        out.writeObject(base);
        out.writeObject(check);
        out.writeObject(fail);
        out.writeObject(output);
        out.writeObject(l);
        out.writeObject(v);
    }

    /**
     * Load data from [ObjectInputStream]
     *
     * @param in An ObjectInputStream object
     * @throws IOException            If can't read the file from path
     * @throws ClassNotFoundException If the class doesn't exist or matched
     */
    public void load(ObjectInputStream in) throws IOException, ClassNotFoundException {
        base = (int[]) in.readObject();
        check = (int[]) in.readObject();
        fail = (int[]) in.readObject();
        output = (int[][]) in.readObject();
        l = (int[]) in.readObject();
        v = (V[]) in.readObject();
    }

    /**
     * Get value by a String key, just like a map.get() method
     *
     * @param key The key
     * @return value if exist otherwise it return null
     */
    public V get(CharSequence key) {
        int index = exactMatchSearch(key);
        if (index >= 0) {
            return v[index];
        }

        return null;
    }

    /**
     * Update a value corresponding to a key
     *
     * @param key   the key
     * @param value the value
     * @return successful or not（failure if there is no key）
     */
    public boolean set(CharSequence key, V value) {
        int index = exactMatchSearch(key);
        if (index >= 0) {
            v[index] = value;
            return true;
        }

        return false;
    }

    /**
     * Pick the value by index in value array <br>
     * Notice that to be more efficiently, this method DO NOT check the parameter
     *
     * @param index The index
     * @return The value
     */
    public V get(int index) {
        return v[index];
    }

    /**
     * transmit state, supports failure function
     *
     * @param currentState current state
     * @param character    character
     * @return state
     */
    private int getState(int currentState, char character) {
        int newCurrentState = transitionWithRoot(currentState, character);
        while (newCurrentState == -1) {
            currentState = fail[currentState];
            newCurrentState = transitionWithRoot(currentState, character);
        }
        return newCurrentState;
    }

    /**
     * store output
     *
     * @param position       position
     * @param currentState   current state
     * @param collectedEmits collected emits
     */
    private void storeEmits(int position, int currentState, List<Hit<V>> collectedEmits) {
        int[] hitArray = output[currentState];
        if (hitArray != null) {
            for (int hit : hitArray) {
                collectedEmits.add(new Hit<V>(position - l[hit], position, v[hit]));
            }
        }
    }

    /**
     * transition of a state
     *
     * @param current current state
     * @param c       character
     * @return next state
     */
    protected int transition(int current, char c) {
        int b = current;
        int p;

        p = b + c + 1;
        if (b == check[p])
            b = base[p];
        else
            return -1;

        p = b;
        return p;
    }

    /**
     * transition of a state, if the state is root and it failed, then returns the root
     *
     * @param nodePos node position
     * @param c       character
     * @return transition
     */
    protected int transitionWithRoot(int nodePos, char c) {
        int b = base[nodePos];
        int p;

        p = b + c + 1;
        if (b != check[p]) {
            if (nodePos == 0) return 0;
            return -1;
        }

        return p;
    }

    /**
     * Build a AhoCorasickDoubleArrayTrie from a map
     *
     * @param map a map containing key-value pairs
     */
    public void build(Map<String, V> map) {
        new Builder().build(map);
    }

    /**
     * match exactly by a key
     *
     * @param key the key
     * @return the index of the key, you can use it as a perfect hash function
     */
    public int exactMatchSearch(CharSequence key) {
        return exactMatchSearch(key, 0, 0, 0);
    }

    /**
     * match exactly by a key
     *
     * @param key     key
     * @param pos     postioon
     * @param len     length
     * @param nodePos node poistion
     * @return position
     */
    private int exactMatchSearch(CharSequence key, int pos, int len, int nodePos) {
        if (len <= 0)
            len = key.length();
        if (nodePos <= 0)
            nodePos = 0;

        int result = -1;


        return getMatched(pos, len, result, key, base[nodePos]);
    }

    private int getMatched(int pos, int len, int result, CharSequence key, int b1) {
        int b = b1;
        int p;

        for (int i = pos; i < len; i++) {
            p = b + (int) (key.charAt(i)) + 1;
            if (b == check[p])
                b = base[p];
            else
                return result;
        }

        p = b; // transition through '\0' to check if it's the end of a word
        int n = base[p];
        if (b == check[p]) {
            result = -n - 1;
        }
        return result;
    }

    /**
     * @return the size of the keywords
     */
    public int size() {
        return v.length;
    }

    @Override
    public V get(String key) {
        return get((CharSequence) key);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        build(new HashMap<>());
    }

    /**
     * Processor handles the output when hit a keyword
     *
     * @param <V> the value type
     */
    public interface IHit<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end   the ending index, exclusive.
         * @param value the value assigned to the keyword
         */
        void hit(int begin, int end, V value);
    }

    /**
     * Processor handles the output when hit a keyword, with more detail
     *
     * @param <V> the value type
     */
    public interface IHitFull<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end   the ending index, exclusive.
         * @param value the value assigned to the keyword
         * @param index the index of the value assigned to the keyword, you can use the integer as a perfect hash value
         */
        void hit(int begin, int end, V value, int index);
    }

    /**
     * Callback that allows to cancel the search process.
     *
     * @param <V> the value type
     */
    public interface IHitCancellable<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end   the ending index, exclusive.
         * @param value the value assigned to the keyword
         * @return Return true for continuing the search and false for stopping it.
         */
        boolean hit(int begin, int end, V value);
    }

    /**
     * A result output
     *
     * @param <V> the value type
     */
    public static class Hit<V> {
        /**
         * the beginning index, inclusive.
         */
        public final int begin;
        /**
         * the ending index, exclusive.
         */
        public final int end;
        /**
         * the value assigned to the keyword
         */
        public final V value;

        public Hit(int begin, int end, V value) {
            this.begin = begin;
            this.end = end;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("[%d:%d]=%s", begin, end, value);
        }
    }

    /**
     * A builder to build the AhoCorasickDoubleArrayTrie
     */
    private class Builder {
        /**
         * the root state of trie
         */
        private State rootState = new State();
        /**
         * whether the position has been used
         */
        private boolean[] used;
        /**
         * the allocSize of the dynamic array
         */
        private int allocSize;
        /**
         * a parameter controls the memory growth speed of the dynamic array
         */
        private int progress;
        /**
         * the next position to check unused memory
         */
        private int nextCheckPos;
        /**
         * the size of the key-pair sets
         */
        private int keySize;

        /**
         * Build from a map
         *
         * @param map a map containing key-value pairs
         */
        @SuppressWarnings("unchecked")
        public void build(Map<String, V> map) {
            // Save the values from the map
            v = (V[]) map.values().toArray();
            l = new int[v.length];
            Set<String> keySet = map.keySet();

            // Build a binary trie tree from the keywords
            addAllKeyword(keySet);
            // Build a double-array trie based on the binary trie tree
            buildDoubleArrayTrie(keySet.size());
            // Cleanup the temporary used array
            used = null;
            // Construct the failure states and merge the output tables
            constructFailureStates();
            // Cleanup the root state as it's no longer needed
            rootState = null;
            // Optimize memory usage
            loseWeight();
        }

        /**
         * fetch siblings of a parent node
         *
         * @param parent   parent node
         * @param siblings parent node's child nodes, i . e . the siblings
         * @return the amount of the siblings
         */
        private int fetch(State parent, List<Map.Entry<Integer, State>> siblings) {
            if (parent.isAcceptable()) {
                State fakeNode = new State(-(parent.getDepth() + 1));
                fakeNode.addEmit(parent.getLargestValueId());
                siblings.add(new AbstractMap.SimpleEntry<Integer, State>(0, fakeNode));
            }
            for (Map.Entry<Character, State> entry : parent.getSuccess().entrySet()) {
                siblings.add(new AbstractMap.SimpleEntry<Integer, State>(entry.getKey() + 1, entry.getValue()));
            }
            return siblings.size();
        }

        /**
         * add a keyword
         *
         * @param keyword a keyword
         * @param index   the index of the keyword
         */
        private void addKeyword(String keyword, int index) {
            State currentState = this.rootState;
            for (Character character : keyword.toCharArray()) {
                currentState = currentState.addState(character);
            }
            currentState.addEmit(index);
            l[index] = keyword.length();
        }

        /**
         * add a collection of keywords
         *
         * @param keywordSet the collection holding keywords
         */
        private void addAllKeyword(Collection<String> keywordSet) {
            int i = 0;
            for (String keyword : keywordSet) {
                addKeyword(keyword, i++);
            }
        }

        /**
         * construct failure table
         */
        private void constructFailureStates() {
            fail = new int[size + 1];
            output = new int[size + 1][];
            Queue<State> queue = new ArrayDeque<State>();

            // Step 1: Set failure state of depth one states to root state
            for (State depthOneState : this.rootState.getStates()) {
                depthOneState.setFailure(this.rootState, fail);
                queue.add(depthOneState);
                constructOutput(depthOneState);
            }

            // Step 2: BFS to build failure states for nodes deeper than depth one
            while (!queue.isEmpty()) {
                State currentState = queue.remove();

                for (Character transition : currentState.getTransitions()) {
                    State targetState = currentState.nextState(transition);
                    queue.add(targetState);

                    State traceFailureState = currentState.failure();
                    while (traceFailureState.nextState(transition) == null) {
                        traceFailureState = traceFailureState.failure();
                    }
                    State newFailureState = traceFailureState.nextState(transition);
                    targetState.setFailure(newFailureState, fail);
                    targetState.addEmit(newFailureState.emit());
                    constructOutput(targetState);
                }
            }
        }

        /**
         * construct output table
         */
        private void constructOutput(State targetState) {
            Collection<Integer> emit = targetState.emit();
            if (emit == null || emit.size() == 0) return;
            int[] output = new int[emit.size()];
            Iterator<Integer> it = emit.iterator();
            for (int i = 0; i < output.length; ++i) {
                output[i] = it.next();
            }
            AhoCorasickDoubleArrayTrie.this.output[targetState.getIndex()] = output;
        }

        private void buildDoubleArrayTrie(int keySize) {
            progress = 0;
            this.keySize = keySize;
            resize(65536 * 32); // 32 double-byte characters

            base[0] = 1;
            nextCheckPos = 0;

            State root_node = this.rootState;

            List<Map.Entry<Integer, State>> siblings = new ArrayList<Map.Entry<Integer, State>>(root_node.getSuccess().entrySet().size());
            fetch(root_node, siblings);
            if (siblings.isEmpty())
                Arrays.fill(check, -1); // fill -1 such that no transition is allowed
            else
                insert(siblings);
        }

        /**
         * allocate the memory of the dynamic array
         *
         * @param newSize of the new array
         * @return the new-allocated-size
         */
        private int resize(int newSize) {
            int[] base2 = new int[newSize];
            int[] check2 = new int[newSize];
            boolean[] used2 = new boolean[newSize];
            if (allocSize > 0) {
                System.arraycopy(base, 0, base2, 0, allocSize);
                System.arraycopy(check, 0, check2, 0, allocSize);
                System.arraycopy(used, 0, used2, 0, allocSize);
            }

            base = base2;
            check = check2;
            used = used2;

            return allocSize = newSize;
        }

        /**
         * insert the siblings to double array trie
         *
         * @param firstSiblings the initial siblings being inserted
         */
        private void insert(List<Map.Entry<Integer, State>> firstSiblings) {
            Queue<Map.Entry<Integer, List<Map.Entry<Integer, State>>>> siblingQueue = new ArrayDeque<Map.Entry<Integer, List<Map.Entry<Integer, State>>>>();
            siblingQueue.add(new AbstractMap.SimpleEntry<Integer, List<Map.Entry<Integer, State>>>(null, firstSiblings));

            while (!siblingQueue.isEmpty()) {
                insert(siblingQueue);
            }
        }

        /**
         * insert the siblings to double array trie
         *
         * @param siblingQueue a queue holding all siblings being inserted and the position to insert them
         */
        private void insert(Queue<Map.Entry<Integer, List<Map.Entry<Integer, State>>>> siblingQueue) {
            Map.Entry<Integer, List<Map.Entry<Integer, State>>> tCurrent = siblingQueue.remove();
            List<Map.Entry<Integer, State>> siblings = tCurrent.getValue();

            int begin = 0;
            int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
            int nonzero_num = 0;
            int first = 0;

            if (allocSize <= pos)
                resize(pos + 1);

            outer:
            // The goal of this loop is to find n free spaces such that base[begin + a1...an] == 0, where a1...an are the n nodes in siblings.
            while (true) {
                pos++;

                if (allocSize <= pos)
                    resize(pos + 1);

                if (check[pos] != 0) {
                    nonzero_num++;
                    continue;
                } else if (first == 0) {
                    nextCheckPos = pos;
                    first = 1;
                }

                begin = pos - siblings.get(0).getKey(); // The distance from the current position to the first sibling node.
                if (allocSize <= (begin + siblings.get(siblings.size() - 1).getKey())) {
                    // progress can be zero
                    double toSize = Math.max(1.05, 1.0 * keySize / (progress + 1)) * allocSize;
                    int maxSize = (int) (Integer.MAX_VALUE * 0.95);
                    if (allocSize >= maxSize) throw new RuntimeException("Double array trie is too big.");
                    else resize((int) Math.min(toSize, maxSize));
                }

                if (used[begin])
                    continue;

                for (int i = 1; i < siblings.size(); i++)
                    if (check[begin + siblings.get(i).getKey()] != 0)
                        continue outer;

                break;
            }

            // -- Simple heuristics --
            // if the percentage of non-empty contents in check between the
            // index
            // 'next_check_pos' and 'check' is greater than some constant value
            // (e.g. 0.9),
            // new 'next_check_pos' index is written by 'check'.
            if (1.0 * nonzero_num / (pos - nextCheckPos + 1) >= 0.95)
                nextCheckPos = pos; // Starting from position next_check_pos to pos, if the occupied space exceeds 95%, the next node insertion should start searching directly from the position pos.
            used[begin] = true;

            size = (size > begin + siblings.get(siblings.size() - 1).getKey() + 1) ? size : begin + siblings.get(siblings.size() - 1).getKey() + 1;

            for (Map.Entry<Integer, State> sibling : siblings) {
                check[begin + sibling.getKey()] = begin;
            }

            for (Map.Entry<Integer, State> sibling : siblings) {
                List<Map.Entry<Integer, State>> new_siblings = new ArrayList<Map.Entry<Integer, State>>(sibling.getValue().getSuccess().entrySet().size() + 1);

                if (fetch(sibling.getValue(), new_siblings) == 0) {
                    // The termination of a word that is not a prefix for any other word is essentially a leaf node.
                    base[begin + sibling.getKey()] = (-sibling.getValue().getLargestValueId() - 1);
                    progress++;
                } else {
                    siblingQueue.add(new AbstractMap.SimpleEntry<Integer, List<Map.Entry<Integer, State>>>(begin + sibling.getKey(), new_siblings));
                }
                sibling.getValue().setIndex(begin + sibling.getKey());
            }

            // Insert siblings
            Integer parentBaseIndex = tCurrent.getKey();
            if (parentBaseIndex != null) {
                base[parentBaseIndex] = begin;
            }
        }

        /**
         * free the unnecessary memory
         */
        private void loseWeight() {
            int[] nbase = new int[size + 65535];
            System.arraycopy(base, 0, nbase, 0, size);
            base = nbase;

            int[] ncheck = new int[size + 65535];
            System.arraycopy(check, 0, ncheck, 0, Math.min(check.length, ncheck.length));
            check = ncheck;
        }
    }
}