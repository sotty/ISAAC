/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.model.sememe.dataTypes;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;

/**
 * {@link DynamicSememeData}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class DynamicSememeDataImpl implements DynamicSememeData
{
	//name_ is a cache of data looked up from the assemblageNid and columnNumber.
	//Note that name is only required by the JavaFX getDataXXXProperty() calls in the subclasses
	//normally, this is never used at all.
	private transient int assemblageSequence_;
	private transient int columnNumber_ = -1;
	private transient String name_;
	
	protected byte[] data_;
	
	protected DynamicSememeDataImpl(byte[] data)
	{
		data_ = data;
	}
	
	protected DynamicSememeDataImpl(byte[] data, int assemblageSequence, int columnNumber)
	{
		data_ = data;
		assemblageSequence_ = assemblageSequence;
		columnNumber_ = columnNumber;
		name_ = null;
	}
	
	protected DynamicSememeDataImpl()
	{
	}
	
	protected String getName()
	{
		if (name_ == null)
		{
			if (columnNumber_ == -1)
			{
				throw new RuntimeException("No data is available to lookup the name.  Has this refex been added to a DynamicSememeCAB via a setData(...) call?");
			}
			else
			{
				DynamicSememeUtility ls =  LookupService.get().getService(DynamicSememeUtility.class);
				if (ls == null)
				{
					throw new RuntimeException("An implementation of DynamicSememeUtiltyBI is not available on the classpath");
				}
				else
				{
					name_ = ls.readDynamicSememeUsageDescription(assemblageSequence_).getColumnInfo()[columnNumber_].getColumnName();
				}
			}
		}
		return name_;
	}
	
	/**
	 * This method is not intended for public use.
	 * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeDataBI#setNameIfAbsent(java.lang.String)
	 */
	protected void setNameIfAbsent(String name)
	{
		if (name_ == null && columnNumber_ == -1)
		{
			name_ = name;
		}
	}
	
	/**
	 * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeDataBI#getDynamicSememeDataType()
	 */
	@Override
	public DynamicSememeDataType getDynamicSememeDataType()
	{
		return DynamicSememeDataType.classToType(this.getClass());
	}
	
	/**
	 * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeDataBI#getData()
	 */
	@Override
	public byte[] getData()
	{
		return data_;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String name;
		try
		{
			name = getName();
		}
		catch (Exception e)
		{
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error reading name", e);
			name = "???";
		}
		
		return "(" + getDynamicSememeDataType().name() + " - " + name + " - " + getDataObject() +")";
	}
	
	@Override
	public String dataToString()
	{
		switch (this.getDynamicSememeDataType())
		{
			case BOOLEAN: case DOUBLE: case FLOAT: case INTEGER: case LONG: case NID: case SEQUENCE: case STRING: case UUID:
				return getDataObject().toString();
			case ARRAY:
				String temp = "[";
				for (DynamicSememeData dsdNest : ((DynamicSememeArray<?>)this).getDataArray())
				{
					temp += dsdNest.dataToString() + ", ";
				}
				if (temp.length() > 1)
				{
					temp = temp.substring(0,  temp.length() - 2);
				}
				temp += "]";
				return temp;
			case BYTEARRAY:
				return "[-byte array size " + data_.length + "]";
			case POLYMORPHIC: case UNKNOWN: 
			default:
				LoggerFactory.getLogger(DynamicSememeDataImpl.class).error("Unexpected case!");
				return "--internal error--";
		}
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data_);
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DynamicSememeDataImpl other = (DynamicSememeDataImpl) obj;
		if (!Arrays.equals(data_, other.data_))
			return false;
		return true;
	}
}
