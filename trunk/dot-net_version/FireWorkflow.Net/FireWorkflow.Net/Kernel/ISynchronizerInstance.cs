﻿/**
 * Copyright 2003-2008 非也
 * All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation。
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 * @author 非也,nychen2000@163.com
 * @Revision to .NET 无忧 lwz0721@gmail.com 2010-02
 */
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using FireWorkflow.Net.Model.Net;

namespace FireWorkflow.Net.Kernel
{
    /// <summary>
    /// Synchronizer的行为特征是：消耗掉输入的token并产生输出token
    /// </summary>
    public interface ISynchronizerInstance : INodeInstance
    {
        /// <summary>
        /// volume是同步器的容量
        /// </summary>
        /// <param name="k"></param>
        void setVolume(int k);
        int getVolume();

        // value是同步器当前的token值之和。
        // （已经转移到JoinPoint中，20080215）
        //	public void setValue(int value);
        //	public int getValue();

        Synchronizer getSynchronizer();
    }
}