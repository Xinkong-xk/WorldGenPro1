package io.xk.worldgenpro.vanilla.enhance.config.api;

import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;

public record ElemConfig(Input_Int_IntList_Str_StrList_To_Int weight,
                         Input_Int_IntList_Str_StrList_To_Int max_count,
                         Input_Str_StrList_To_Null script,
                         Input_Int_IntList_Str_StrList_To_Int min_count) {}