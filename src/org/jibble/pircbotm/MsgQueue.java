/*
 * Created on 22.11.2006
 *
 * (c) K.Baturytski
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * $Revision: 1.1 $
 */
package org.jibble.pircbotm;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Rotating message queue. Provides simple fairness strategy for selecting
 * messages from the queue.
 * 
 * Not intended for use in multi-threaded environment (requires external sync).
 * 
 * @author K.Baturytski
 */
class MsgQueue extends AbstractQueue<IRCMsg> {

    /** target->message queue mapping */
    private Map<String, Queue<IRCMsg>> messages = new HashMap<String, Queue<IRCMsg>>();

    /** list of targets */
    private List<String> targets = new ArrayList<String>();

    /** next bucket index */
    private int nextPos = 0;

    @Override
    public Iterator<IRCMsg> iterator() {
        return new Iterator<IRCMsg>() {
            private int pos = -1;

            private IRCMsg[] storage = new IRCMsg[size()];
            {
                int spos = 0;
                for (int i = 0; i < targets.size(); i++) {
                    Queue<IRCMsg> bucket = messages.get(targets.get(i));
                    System.arraycopy(bucket.toArray(), 0, storage, spos, bucket.size());
                    spos += bucket.size();
                }

            }

            public boolean hasNext() {
                return pos < size();
            }

            public IRCMsg next() {
                if (++pos >= size()) {
                    throw new NoSuchElementException();
                }

                return storage[pos];
            }

            public void remove() {
                // do nothing
            }
        };
    }

    @Override
    public int size() {
        int result = 0;
        for (Queue<IRCMsg> list : messages.values()) {
            result += list.size();
        }
        return result;
    }

    /**
     * @see java.util.Queue#offer(Object)
     */
    public boolean offer(IRCMsg o) {
        if (!targets.contains(o.getTarget())) {
            targets.add(o.getTarget());
        }
        Queue<IRCMsg> bucket = messages.get(o.getTarget());
        if (bucket == null) {
            bucket = new LinkedList<IRCMsg>();
            messages.put(o.getTarget(), bucket);
        }
        bucket.add(o);
        return true;
    }

    /**
     * @see java.util.Queue#peek()
     */
    public IRCMsg peek() {
        if (size() == 0) {
            return null;
        }

        return messages.get(targets.get(nextPos)).peek();
    }

    /**
     * @see java.util.Queue#poll()
     */
    public IRCMsg poll() {
        if (size() == 0) {
            return null;
        }
        int pos = nextPos();
        Queue<IRCMsg> bucket = messages.get(targets.get(pos));
        IRCMsg msg = bucket.poll();
        if (bucket.size() == 0) {
            targets.remove(pos);
        }
        return msg;
    }

    /**
     * Generates next bucket position.
     * 
     * @return
     */
    private int nextPos() {
        nextPos = (nextPos + 1) % (targets.size());
        return nextPos;
    }

}
