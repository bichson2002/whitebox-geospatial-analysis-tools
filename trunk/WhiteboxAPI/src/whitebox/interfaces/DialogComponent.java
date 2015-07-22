/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package whitebox.interfaces;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public interface DialogComponent {
    public String[] getArgsDescriptors();
    public String getComponentName();
    public boolean getOptionalStatus();
    public String getValue();
    public boolean setArgs(String[] args);
    public String getLabel();
    public void setLabel(String label);
}
