import { Routes, Route } from "./routes";
import { AclEditorProps } from "./acleditor";
import { TemplateProps } from "./Template";

export interface Bridge {
    routes: Routes,
    router: (route: Route) => {
        href: string;
        onClick: (e: React.MouseEvent<HTMLAnchorElement>) => void;
    },
    forcePushRoute: (Route: Route) => void;
    Template: React.ComponentType<TemplateProps>
    AclEditor: React.ComponentType<AclEditorProps>
}