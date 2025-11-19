package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

abstract class IDataFlowKey : GeneratedDeclarationKey()

data object DataFlowKey : IDataFlowKey()

data class DataFlowPropertyKey(
    val ownerClassSymbol: FirClassSymbol<*>,
    val constructorSymbol: FirConstructorSymbol,
    val parameterSymbol: FirValueParameterSymbol
) : IDataFlowKey()

data class DataFlowFunctionKey(
    val ownerClassSymbol: FirClassSymbol<*>,
    val constructorSymbol: FirConstructorSymbol,
    val parameterSymbol: FirValueParameterSymbol
) : IDataFlowKey()

data class DataFlowClassKey(
    val ownerClassSymbol: FirClassSymbol<*>,
    val constructorSymbol: FirConstructorSymbol
) : IDataFlowKey()
