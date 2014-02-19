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

/**
 * Holds message target and body.
 * 
 * @author K.Baturytski
 */
class IRCMsg {
    /** message target */
    private final String target;

    /** message body */
    private final String message;

    /**
     * Constructor.
     * 
     * @param target
     * @param message
     */
    public IRCMsg(String target, String message) {
        super();
        this.target = target;
        this.message = message;
    }

    /**
     * Returns the message.
     * 
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the target.
     * 
     * @return
     */
    public String getTarget() {
        return target;
    }

    public String toString() {
        return this.getClass().getName() + "[t=" + target + "; m=" + message;
    }

}
